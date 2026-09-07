package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.domain.model.EvaluationCreateRequest
import restarhalf.stellar.schedule.domain.model.EvaluationPage
import restarhalf.stellar.schedule.domain.model.EvaluationUpdateRequest
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.LikeResult
import restarhalf.stellar.schedule.domain.port.CourseEvaluationPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * CourseEvaluationViewModel 单元测试。
 *
 * uiState 是裸 MutableStateFlow，无需订阅即可读 value。
 * userHash 用 CourseEvaluationPort.hashUserNo 真实计算（JVM 有 optimal provider），
 * 断言时对同一学号重新计算比对，不硬编码 hash 值。
 */
class CourseEvaluationViewModelTest {

    private val port = mock<CourseEvaluationPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    /** init 即 collect 档案流，类级持有以便测试中切换登录态 */
    private val profileFlow = MutableStateFlow(JwxtAuthProfile())

    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        profileFlow.value = JwxtAuthProfile()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun evaluation(
        id: String,
        userHash: String = "",
        courseName: String = "高等数学",
        content: String = "讲得不错",
        author: String = "小明",
        rating: Int = 4,
        likes: Int = 0,
        liked: Boolean = false,
    ) = Evaluation(
        id = id,
        courseName = courseName,
        teacher = "张三",
        rating = rating,
        content = content,
        author = author,
        userHash = userHash,
        likes = likes,
        liked = liked,
    )

    /** init 三路 stub（昵称/档案流/已选课程）必须先于 VM 构造 */
    private fun makeViewModel(): CourseEvaluationViewModel {
        every { settings.getUserNickname() } returns null
        every { auth.observeProfile() } returns profileFlow
        everySuspend { courseRepository.getAllCoursesAcrossSemesters() } returns emptyList()
        return CourseEvaluationViewModel(port, courseRepository, auth, settings)
    }

    /** 已登录档案（学号 20230001） */
    private fun loggedInProfile() =
        JwxtAuthProfile(name = "小明", userNo = "20230001")

    // region init

    @Test
    fun `init读取昵称与档案并计算userHash`() = runTest {
        every { settings.getUserNickname() } returns "自定义昵称"
        every { auth.observeProfile() } returns MutableStateFlow(loggedInProfile())
        everySuspend { courseRepository.getAllCoursesAcrossSemesters() } returns emptyList()

        val vm = CourseEvaluationViewModel(port, courseRepository, auth, settings)
        advanceMain()

        assertEquals("自定义昵称", vm.uiState.value.userNickname)
        assertEquals("20230001", vm.uiState.value.userNo)
        assertEquals("小明", vm.uiState.value.profileName)
        assertEquals(
            CourseEvaluationPort.hashUserNo("20230001"),
            vm.uiState.value.userHash,
        )
    }

    @Test
    fun `init未登录时userHash为空`() = runTest {
        val vm = makeViewModel()
        advanceMain()

        assertEquals("", vm.uiState.value.userNo)
        assertEquals("", vm.uiState.value.userHash)
    }

    @Test
    fun `init加载已选课程`() = runTest {
        every { settings.getUserNickname() } returns null
        every { auth.observeProfile() } returns MutableStateFlow(JwxtAuthProfile())
        everySuspend { courseRepository.getAllCoursesAcrossSemesters() } returns
            listOf(
                Course(
                    name = "高等数学",
                    location = "一教",
                    teacher = "张三",
                    dayOfWeek = 1,
                    startSection = 1,
                    sectionCount = 2,
                    weeks = listOf(1),
                    color = "#FF0000",
                ),
            )

        val vm = CourseEvaluationViewModel(port, courseRepository, auth, settings)
        advanceMain()

        assertEquals(1, vm.uiState.value.myCourses.size)
        assertEquals("高等数学", vm.uiState.value.myCourses[0].name)
    }

    @Test
    fun `init加载已选课程失败不影响状态`() = runTest {
        every { settings.getUserNickname() } returns null
        every { auth.observeProfile() } returns MutableStateFlow(JwxtAuthProfile())
        everySuspend { courseRepository.getAllCoursesAcrossSemesters() } throws
            RuntimeException("db error")

        val vm = CourseEvaluationViewModel(port, courseRepository, auth, settings)
        advanceMain()

        assertTrue(vm.uiState.value.myCourses.isEmpty())
        assertNull(vm.uiState.value.error)
    }

    // endregion

    // region loadEvaluations

    @Test
    fun `loadEvaluations成功填充列表与total`() = runTest {
        val vm = makeViewModel()
        everySuspend {
            port.listEvaluations(any(), any(), any(), any())
        } returns EvaluationPage(
            items = listOf(evaluation("e1"), evaluation("e2")),
            total = 2,
        )

        vm.loadEvaluations()
        advanceMain()

        assertEquals(2, vm.uiState.value.evaluations.size)
        assertEquals(2, vm.uiState.value.total)
        assertFalse(vm.uiState.value.loading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `loadEvaluations失败设置error`() = runTest {
        val vm = makeViewModel()
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } throws
            RuntimeException("网络错误")

        vm.loadEvaluations()
        advanceMain()

        assertEquals("网络错误", vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `loadEvaluations带参设置筛选并透传port`() = runTest {
        val vm = makeViewModel()
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns EvaluationPage()

        vm.loadEvaluations("大学物理", "李四")
        advanceMain()

        assertEquals("大学物理", vm.uiState.value.selectedCourse)
        assertEquals("李四", vm.uiState.value.selectedTeacher)
        verifySuspend {
            port.listEvaluations("大学物理", "李四", any(), any())
        }
    }

    @Test
    fun `loadEvaluations带参teacher未知也透传`() = runTest {
        val vm = makeViewModel()
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns EvaluationPage()

        vm.loadEvaluations("大学物理", "教师未知")
        advanceMain()

        assertEquals("教师未知", vm.uiState.value.selectedTeacher)
        verifySuspend {
            port.listEvaluations("大学物理", "教师未知", any(), any())
        }
    }

    // endregion

    // region loadCourseSummaries / loadDetail

    @Test
    fun `loadCourseSummaries成功填充聚合列表`() = runTest {
        val vm = makeViewModel()
        everySuspend { port.listCourseSummaries() } returns listOf(
            CourseEvaluationSummary(courseName = "高等数学", avgRating = 4.5, evalCount = 3),
        )

        vm.loadCourseSummaries()
        advanceMain()

        assertEquals(1, vm.uiState.value.courseSummaries.size)
        assertEquals("高等数学", vm.uiState.value.courseSummaries[0].courseName)
        assertEquals(4.5, vm.uiState.value.courseSummaries[0].avgRating)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `loadCourseSummaries失败设置error`() = runTest {
        val vm = makeViewModel()
        everySuspend { port.listCourseSummaries() } throws RuntimeException("服务不可用")

        vm.loadCourseSummaries()
        advanceMain()

        assertEquals("服务不可用", vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `loadDetail成功设置selectedEvaluation`() = runTest {
        val vm = makeViewModel()
        everySuspend { port.getEvaluation("e1") } returns evaluation("e1")

        vm.loadDetail("e1")
        advanceMain()

        assertEquals("e1", vm.uiState.value.selectedEvaluation?.id)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `loadDetail失败设置error`() = runTest {
        val vm = makeViewModel()
        everySuspend { port.getEvaluation("e1") } throws RuntimeException("不存在")

        vm.loadDetail("e1")
        advanceMain()

        assertEquals("不存在", vm.uiState.value.error)
        assertNull(vm.uiState.value.selectedEvaluation)
    }

    // endregion

    // region submitEvaluation

    @Test
    fun `submit未登录守卫不调port`() = runTest {
        val vm = makeViewModel()
        advanceMain()

        vm.submitEvaluation(EvaluationCreateRequest(courseName = "高数", rating = 5, content = "好"))
        advanceMain()

        assertEquals("请先登录教务系统后再提交评价", vm.uiState.value.error)
        verifySuspend(VerifyMode.exactly(0)) {
            port.createEvaluation(any())
        }
    }

    @Test
    fun `submit成功设置successMessage`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.createEvaluation(any()) } returns evaluation("new1")

        vm.submitEvaluation(EvaluationCreateRequest(courseName = "高数", rating = 5, content = "好"))
        advanceMain()

        assertEquals("提交成功", vm.uiState.value.successMessage)
        assertFalse(vm.uiState.value.submitting)
        verifySuspend(VerifyMode.exactly(1)) {
            port.createEvaluation(any())
        }
    }

    @Test
    fun `submit失败设置error`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.createEvaluation(any()) } throws RuntimeException("后端拒绝")

        vm.submitEvaluation(EvaluationCreateRequest(courseName = "高数", rating = 5, content = "好"))
        advanceMain()

        assertEquals("后端拒绝", vm.uiState.value.error)
        assertFalse(vm.uiState.value.submitting)
    }

    // endregion

    // region deleteEvaluation

    @Test
    fun `delete未登录守卫回调false不调port`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        var callbackResult: Boolean? = null

        vm.deleteEvaluation("e1") { callbackResult = it }
        advanceMain()

        assertEquals("请先登录教务系统后再操作", vm.uiState.value.error)
        assertEquals(false, callbackResult)
        verifySuspend(VerifyMode.exactly(0)) {
            port.deleteEvaluation(any())
        }
    }

    @Test
    fun `delete成功回调true并重新加载列表`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.deleteEvaluation("e1") } returns true
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns EvaluationPage()
        var callbackResult: Boolean? = null

        vm.deleteEvaluation("e1") { callbackResult = it }
        advanceMain()

        assertEquals("已删除", vm.uiState.value.successMessage)
        assertEquals(true, callbackResult)
        verifySuspend(VerifyMode.exactly(1)) { port.deleteEvaluation("e1") }
        // 成功路径会触发 loadEvaluations() 重新拉列表
        verifySuspend(VerifyMode.exactly(1)) {
            port.listEvaluations(any(), any(), any(), any())
        }
    }

    @Test
    fun `delete抛异常回调false并设置error`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.deleteEvaluation("e1") } throws RuntimeException("无权限")
        var callbackResult: Boolean? = null

        vm.deleteEvaluation("e1") { callbackResult = it }
        advanceMain()

        assertEquals(false, callbackResult)
        assertEquals("无权限", vm.uiState.value.error)
    }

    // endregion

    // region toggleLike

    @Test
    fun `toggleLike未登录守卫不调port`() = runTest {
        val vm = makeViewModel()
        advanceMain()

        vm.toggleLike("e1")
        advanceMain()

        assertEquals("请先登录教务系统后再点赞", vm.uiState.value.error)
        verifySuspend(VerifyMode.exactly(0)) {
            port.toggleLike(any())
        }
    }

    @Test
    fun `toggleLike成功更新列表与选中项`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns
            EvaluationPage(items = listOf(evaluation("e1"), evaluation("e2")))
        everySuspend { port.getEvaluation("e1") } returns evaluation("e1")
        everySuspend { port.toggleLike("e1") } returns LikeResult(likes = 6, liked = true)

        vm.loadEvaluations()
        vm.loadDetail("e1")
        advanceMain()
        vm.toggleLike("e1")
        advanceMain()

        assertEquals(6, vm.uiState.value.evaluations[0].likes)
        assertTrue(vm.uiState.value.evaluations[0].liked)
        // e2 不受影响
        assertEquals(0, vm.uiState.value.evaluations[1].likes)
        // 选中项同步更新
        assertEquals(6, vm.uiState.value.selectedEvaluation?.likes)
        assertTrue(vm.uiState.value.selectedEvaluation?.liked == true)
    }

    @Test
    fun `toggleLike失败设置error`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.toggleLike("e1") } throws RuntimeException("点赞失败")

        vm.toggleLike("e1")
        advanceMain()

        assertEquals("点赞失败", vm.uiState.value.error)
    }

    // endregion

    // region updateEvaluation

    @Test
    fun `update未登录守卫不调port`() = runTest {
        val vm = makeViewModel()
        advanceMain()

        vm.updateEvaluation("e1", EvaluationUpdateRequest(rating = 2))
        advanceMain()

        assertEquals("请先登录教务系统后再编辑评价", vm.uiState.value.error)
        verifySuspend(VerifyMode.exactly(0)) {
            port.updateEvaluation(any(), any())
        }
    }

    @Test
    fun `update成功替换列表项与选中项`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns
            EvaluationPage(items = listOf(evaluation("e1"), evaluation("e2")))
        everySuspend { port.getEvaluation("e1") } returns evaluation("e1")
        val updated = evaluation("e1", content = "改后的内容", rating = 2)
        everySuspend { port.updateEvaluation("e1", any()) } returns updated

        vm.loadEvaluations()
        vm.loadDetail("e1")
        advanceMain()
        vm.updateEvaluation("e1", EvaluationUpdateRequest(rating = 2, content = "改后的内容"))
        advanceMain()

        assertEquals("编辑成功", vm.uiState.value.successMessage)
        assertFalse(vm.uiState.value.submitting)
        assertEquals("改后的内容", vm.uiState.value.evaluations[0].content)
        assertEquals(2, vm.uiState.value.evaluations[0].rating)
        // e2 不受影响
        assertEquals("讲得不错", vm.uiState.value.evaluations[1].content)
        // 选中项替换为更新后的评价
        assertEquals("改后的内容", vm.uiState.value.selectedEvaluation?.content)
    }

    @Test
    fun `update失败设置error`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.updateEvaluation(any(), any()) } throws RuntimeException("无权限")

        vm.updateEvaluation("e1", EvaluationUpdateRequest(rating = 2))
        advanceMain()

        assertEquals("无权限", vm.uiState.value.error)
        assertFalse(vm.uiState.value.submitting)
    }

    // endregion

    // region 筛选与计算属性

    @Test
    fun `setOnlyMine与onSearchQueryChange更新状态`() = runTest {
        val vm = makeViewModel()

        vm.setOnlyMine(true)
        vm.onSearchQueryChange("高数")

        assertTrue(vm.uiState.value.onlyMine)
        assertEquals("高数", vm.uiState.value.searchQuery)
    }

    @Test
    fun `filteredEvaluations按onlyMine过滤本机userHash`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        val mineHash = CourseEvaluationPort.hashUserNo("20230001")
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns
            EvaluationPage(items = listOf(evaluation("mine", userHash = mineHash), evaluation("other", userHash = "abc")))

        vm.loadEvaluations()
        advanceMain()
        assertEquals(2, vm.uiState.value.filteredEvaluations.size)

        vm.setOnlyMine(true)

        assertEquals(1, vm.uiState.value.filteredEvaluations.size)
        assertEquals("mine", vm.uiState.value.filteredEvaluations[0].id)
    }

    @Test
    fun `filteredEvaluations按搜索词匹配课程内容与作者`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns
            EvaluationPage(
                items = listOf(
                    evaluation("hit-course", courseName = "大学物理"),
                    evaluation("hit-content", content = "物理很有趣"),
                    evaluation("hit-author", author = "物理课代表"),
                    evaluation("miss", courseName = "高等数学", content = "很棒", author = "小明"),
                ),
            )

        vm.loadEvaluations()
        advanceMain()
        vm.onSearchQueryChange("物理")

        val ids = vm.uiState.value.filteredEvaluations.map { it.id }
        assertEquals(listOf("hit-course", "hit-content", "hit-author"), ids)
    }

    @Test
    fun `filteredCourseSummaries按搜索词匹配课程与教师`() = runTest {
        val vm = makeViewModel()
        everySuspend { port.listCourseSummaries() } returns listOf(
            CourseEvaluationSummary(courseName = "高等数学", teacher = "张三"),
            CourseEvaluationSummary(courseName = "大学物理", teacher = "李四"),
        )

        vm.loadCourseSummaries()
        advanceMain()
        vm.onSearchQueryChange("李四")

        assertEquals(1, vm.uiState.value.filteredCourseSummaries.size)
        assertEquals("大学物理", vm.uiState.value.filteredCourseSummaries[0].courseName)
    }

    @Test
    fun `canDeleteSelected本人评价为true`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()
        val mineHash = CourseEvaluationPort.hashUserNo("20230001")
        everySuspend { port.getEvaluation("e1") } returns evaluation("e1", userHash = mineHash)

        vm.loadDetail("e1")
        advanceMain()

        assertTrue(vm.uiState.value.canDeleteSelected)
    }

    @Test
    fun `canDeleteSelected非本人或未选中为false`() = runTest {
        profileFlow.value = loggedInProfile()
        val vm = makeViewModel()
        advanceMain()

        // 未选中
        assertFalse(vm.uiState.value.canDeleteSelected)

        everySuspend { port.getEvaluation("e1") } returns evaluation("e1", userHash = "someone-else")
        vm.loadDetail("e1")
        advanceMain()

        // 非本人
        assertFalse(vm.uiState.value.canDeleteSelected)
    }

    // endregion
}
