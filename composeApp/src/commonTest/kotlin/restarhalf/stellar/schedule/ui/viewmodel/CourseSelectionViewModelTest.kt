package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
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
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSelectedCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionClassification
import restarhalf.stellar.schedule.data.remote.JwxtSelectionCourse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionOperResult
import restarhalf.stellar.schedule.data.remote.JwxtSelectionResponse
import restarhalf.stellar.schedule.data.remote.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ServiceLogEntry
import restarhalf.stellar.schedule.domain.port.SnatchServiceConfig
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * CourseSelectionViewModel 单元测试。
 *
 * CourseSelectionUseCase 是 final class，用真实实例 + mock JwxtGateway 走完整委托链；
 * session 是 VM 私有状态，只能通过 selectRotation → initSession（成功响应）设置。
 * gateway 响应的 data 是 JsonElement，用 JSON 字符串构造模拟教务响应。
 * 抢课循环在 UnconfinedTestDispatcher 下 delay 走虚拟时间，advanceUntilIdle 即可推进。
 */
class CourseSelectionViewModelTest {

    private val gateway = mock<JwxtGateway>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val servicePort = mock<CourseSelectionServicePort>(MockMode.autofill)

    private val runningFlow = MutableStateFlow(false)
    private val latestLogFlow = MutableStateFlow(ServiceLogEntry(""))

    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        runningFlow.value = false
        latestLogFlow.value = ServiceLogEntry("")
        every { servicePort.running } returns runningFlow
        every { servicePort.latestLog } returns latestLogFlow
        every { servicePort.isSupported } returns false
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region 数据构造

    private fun selectionCourse(
        courseId: String = "k1",
        noticeId: String = "n1",
        kxh: String = "01",
        courseName: String = "高等数学",
    ) = JwxtSelectionCourse(
        courseId = courseId,
        noticeId = noticeId,
        kxh = kxh,
        courseName = courseName,
        classTeacher = "张老师",
        splitIdentification = "s1",
    )

    private fun okResponse(dataJson: String? = null) = JwxtSelectionResponse(
        errorCode = "success",
        data = dataJson?.let { Json.parseToJsonElement(it) },
    )

    private fun rotationsResponse() = okResponse(
        """[{"rotationid":"r1","rotationname":"第一轮"},{"rotationid":"r2","rotationname":"第二轮"}]""",
    )

    private fun sessionResponse(
        sessionTime: String = "st-1",
        classifications: String = """[{"classificationCode":"c1","classificationName":"通识"},{"classificationCode":"c2","classificationName":"体育"}]""",
    ) = okResponse("""{"sessionTime":"$sessionTime","classificationList":$classifications}""")

    private fun coursesResponse() = okResponse(
        """[{"courseName":"高等数学","courseId":"k1","noticeId":"n1","kxh":"01","classTeacher":"张老师","splitIdentification":"s1"}]""",
    )

    private fun selectedCoursesResponse() = okResponse(
        """[{"courseName":"体育","noticeId":"n9"},{"courseName":"大学英语","noticeId":"n8"}]""",
    )

    /** stub 成功会话初始化（分类列表 + sessionTime） */
    private fun stubSessionInit(rotationId: String = "r1", sessionTime: String = "st-1") {
        everySuspend { gateway.initSelectionSession(rotationId) } returns sessionResponse(sessionTime)
        everySuspend { gateway.fetchSelectedCourses(any()) } returns selectedCoursesResponse()
    }

    private fun makeViewModel(): CourseSelectionViewModel =
        CourseSelectionViewModel(
            useCase = CourseSelectionUseCase(gateway, authWorkflow),
            servicePort = servicePort,
        )

    /** 走真实链路设置 VM 私有 session：selectRotation → initSession 成功 → 自动选分类 */
    private fun enterSession(vm: CourseSelectionViewModel, rotationId: String = "r1") {
        stubSessionInit(rotationId)
        vm.selectRotation(rotationId)
        advanceMain()
    }

    // endregion

    // region init 与后台服务

    @Test
    fun `init收集后台服务running状态`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        assertFalse(vm.uiState.value.backgroundRunning)
        assertFalse(vm.uiState.value.snatching)

        runningFlow.value = true
        advanceMain()

        assertTrue(vm.uiState.value.backgroundRunning)
        assertTrue(vm.uiState.value.snatching)
    }

    @Test
    fun `init收集后台日志追加到logs`() = runTest {
        val vm = makeViewModel()
        advanceMain()

        latestLogFlow.value = ServiceLogEntry("服务已启动", ServiceLogEntry.LEVEL_SUCCESS)
        latestLogFlow.value = ServiceLogEntry("", ServiceLogEntry.LEVEL_ERROR) // 空消息被忽略
        advanceMain()

        val logs = vm.uiState.value.logs
        assertEquals(1, logs.size)
        assertEquals("服务已启动", logs[0].message)
        assertEquals(CourseSelectionViewModel.SelectionLog.LogLevel.SUCCESS, logs[0].level)
    }

    @Test
    fun `backgroundSupported来自servicePort`() = runTest {
        every { servicePort.isSupported } returns true
        val vm = makeViewModel()

        assertTrue(vm.uiState.value.backgroundSupported)
    }

    // endregion

    // region loadRotations / selectRotation

    @Test
    fun `loadRotations成功填充并自动选中首轮`() = runTest {
        val vm = makeViewModel()
        stubSessionInit("r1")
        everySuspend { gateway.fetchSelectionRotations(any()) } returns rotationsResponse()

        vm.loadRotations()
        advanceMain()

        assertEquals(2, vm.uiState.value.rotations.size)
        assertEquals("第一轮", vm.uiState.value.rotations[0].rotationName)
        // 自动选中首轮并初始化会话
        assertEquals("r1", vm.uiState.value.selectedRotationId)
        assertTrue(vm.uiState.value.sessionReady)
    }

    @Test
    fun `loadRotations失败设置error`() = runTest {
        val vm = makeViewModel()
        everySuspend { gateway.fetchSelectionRotations(any()) } returns
            JwxtSelectionResponse(errorCode = "error_1", errorMessage = "网络故障")

        vm.loadRotations()
        advanceMain()

        assertEquals("网络故障", vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `selectRotation成功初始化会话并自动选分类`() = runTest {
        val vm = makeViewModel()
        stubSessionInit("r1", "st-9")
        everySuspend { gateway.fetchSelectedCourses(any()) } returns selectedCoursesResponse()

        vm.selectRotation("r1")
        advanceMain()

        assertTrue(vm.uiState.value.sessionReady)
        assertEquals(2, vm.uiState.value.classifications.size)
        // 自动选中第一个分类
        assertEquals("c1", vm.uiState.value.selectedClassificationCode)
        // 会话就绪后自动加载已选课程
        assertEquals(2, vm.uiState.value.selectedCourses.size)
    }

    @Test
    fun `selectRotation失败设置error`() = runTest {
        val vm = makeViewModel()
        everySuspend { gateway.initSelectionSession(any()) } returns
            JwxtSelectionResponse(errorCode = "error_2", errorMessage = "轮次不存在")

        vm.selectRotation("r1")
        advanceMain()

        assertFalse(vm.uiState.value.sessionReady)
        assertEquals("轮次不存在", vm.uiState.value.error)
    }

    // endregion

    // region 分类与搜索

    @Test
    fun `selectClassification加载课程列表`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns
            coursesResponse()

        vm.selectClassification("c2")
        advanceMain()

        assertEquals("c2", vm.uiState.value.selectedClassificationCode)
        assertEquals(1, vm.uiState.value.courses.size)
        assertEquals("高等数学", vm.uiState.value.courses[0].courseName)
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.fetchSelectionCourses(any(), "c2", any(), any(), any())
        }
    }

    @Test
    fun `onCourseSearchQueryChange防抖后带关键词重载`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns
            coursesResponse()
        vm.selectClassification("c1")
        advanceMain()

        // 连续两次输入，前一次防抖 job 被取消，只有最后一次真正发请求
        vm.onCourseSearchQueryChange("高")
        vm.onCourseSearchQueryChange("高数")
        advanceMain()

        assertEquals("高数", vm.uiState.value.courseSearchQuery)
        verifySuspend(VerifyMode.exactly(1)) {
            gateway.fetchSelectionCourses(any(), "c1", any(), any(), "高数")
        }
    }

    @Test
    fun `clearSearch清空关键词并重载`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.fetchSelectionCourses(any(), any(), any(), any(), any()) } returns
            coursesResponse()
        vm.onCourseSearchQueryChange("高数")
        advanceMain()

        vm.clearSearch()
        advanceMain()

        assertEquals("", vm.uiState.value.courseSearchQuery)
        // selectClassification 一次 + clearSearch 一次，都是空关键词
        verifySuspend(VerifyMode.exactly(2)) {
            gateway.fetchSelectionCourses(any(), "c1", any(), any(), "")
        }
    }

    // endregion

    // region addTargetWithCheck

    @Test
    fun `addTarget无分类时提示先选分类`() = runTest {
        val vm = makeViewModel()

        vm.addTargetWithCheck(selectionCourse())

        assertEquals("请先选择选课分类", vm.uiState.value.error)
    }

    @Test
    fun `addTarget试探成功直接加入并标记已成功`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Success("选课成功")

        vm.addTargetWithCheck(selectionCourse())
        advanceMain()

        assertEquals(1, vm.uiState.value.targets.size)
        assertTrue(vm.uiState.value.targets[0].succeeded)
        assertEquals("选课成功", vm.uiState.value.targets[0].lastMessage)
        assertFalse(vm.uiState.value.checkingTarget)
    }

    @Test
    fun `addTarget容量满加入目标继续抢`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Fail("人数已满")

        vm.addTargetWithCheck(selectionCourse())
        advanceMain()

        assertEquals(1, vm.uiState.value.targets.size)
        assertFalse(vm.uiState.value.targets[0].succeeded)
        assertEquals("人数已满", vm.uiState.value.targets[0].lastMessage)
    }

    @Test
    fun `addTarget其他失败不加入只报错`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Fail("上课时间冲突")

        vm.addTargetWithCheck(selectionCourse())
        advanceMain()

        assertTrue(vm.uiState.value.targets.isEmpty())
        assertEquals("上课时间冲突", vm.uiState.value.error)
    }

    @Test
    fun `addTarget重复课程不重复加入`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Fail("人数已满")

        vm.addTargetWithCheck(selectionCourse())
        advanceMain()
        vm.addTargetWithCheck(selectionCourse()) // 同 key
        advanceMain()

        assertEquals(1, vm.uiState.value.targets.size)
    }

    // endregion

    // region 目标排序与配置

    @Test
    fun `removeTarget按键移除`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Fail("人数已满")
        vm.addTargetWithCheck(selectionCourse(courseId = "k1", noticeId = "n1"))
        vm.addTargetWithCheck(selectionCourse(courseId = "k2", noticeId = "n2"))
        advanceMain()
        assertEquals(2, vm.uiState.value.targets.size)

        val target = vm.uiState.value.targets[0]
        vm.removeTarget(target)

        assertEquals(1, vm.uiState.value.targets.size)
        assertEquals("k2|n2|01", vm.uiState.value.targets[0].key)
    }

    @Test
    fun `moveTargetUp与Down调整优先级`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Fail("人数已满")
        vm.addTargetWithCheck(selectionCourse(courseId = "k1", noticeId = "n1"))
        vm.addTargetWithCheck(selectionCourse(courseId = "k2", noticeId = "n2"))
        advanceMain()

        vm.moveTargetUp(1)
        assertEquals("k2|n2|01", vm.uiState.value.targets[0].key)

        vm.moveTargetDown(0)
        assertEquals("k2|n2|01", vm.uiState.value.targets[1].key)

        // 边界：index=0 上移无效，末位下移无效，列表保持 [k1, k2]
        vm.moveTargetUp(0)
        vm.moveTargetDown(1)
        assertEquals("k1|n1|01", vm.uiState.value.targets[0].key)
    }

    @Test
    fun `clearLogs与updateSnatchConfig`() = runTest {
        val vm = makeViewModel()
        advanceMain()
        latestLogFlow.value = ServiceLogEntry("服务日志", ServiceLogEntry.LEVEL_INFO)
        advanceMain()
        assertTrue(vm.uiState.value.logs.isNotEmpty())

        vm.clearLogs()
        assertTrue(vm.uiState.value.logs.isEmpty())

        val config = CourseSelectionViewModel.SnatchConfig(intervalMs = 100L, maxAttempts = 5)
        vm.updateSnatchConfig(config)
        assertEquals(config, vm.uiState.value.snatchConfig)
    }

    // endregion

    // region startSnatch / stopSnatch

    @Test
    fun `startSnatch无目标提示`() = runTest {
        val vm = makeViewModel()

        vm.startSnatch()

        assertEquals("请先添加抢课目标", vm.uiState.value.error)
        assertFalse(vm.uiState.value.snatching)
    }

    @Test
    fun `startSnatch目标全部成功后走pending空break`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Success("选课成功")
        vm.addTargetWithCheck(selectionCourse())
        advanceMain()
        // maxAttempts=0（无限），循环靠"所有目标已成功"退出
        vm.startSnatch()
        advanceMain()

        assertFalse(vm.uiState.value.snatching)
        assertTrue(vm.uiState.value.targets[0].succeeded)
        assertTrue(vm.uiState.value.logs.any { it.message.contains("所有目标已选课成功") })
    }

    @Test
    fun `startSnatch达到maxAttempts后循环停止`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Fail("人数已满")
        vm.addTargetWithCheck(selectionCourse())
        advanceMain()
        vm.updateSnatchConfig(CourseSelectionViewModel.SnatchConfig(maxAttempts = 1))

        vm.startSnatch()
        advanceMain()

        assertFalse(vm.uiState.value.snatching)
        // addTarget 试探 1 次 + 抢课循环 1 次（maxAttempts=1 后 break）
        verifySuspend(VerifyMode.exactly(2)) {
            gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        }
        // 结束日志存在
        assertTrue(vm.uiState.value.logs.any { it.message.contains("抢课循环结束") })
    }

    @Test
    fun `stopSnatch复位状态并记录日志`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Fail("人数已满")
        vm.addTargetWithCheck(selectionCourse())
        advanceMain()

        vm.startSnatch()
        vm.stopSnatch() // 循环挂起中直接取消，不 advanceUntilIdle 避免无限循环

        assertFalse(vm.uiState.value.snatching)
        assertTrue(vm.uiState.value.logs.any { it.message.contains("已停止抢课") })
    }

    // endregion

    // region 后台抢课

    @Test
    fun `startBackgroundSnatch平台不支持返回false`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)

        val ok = vm.startBackgroundSnatch()

        assertFalse(ok)
        assertEquals("当前平台不支持后台抢课", vm.uiState.value.error)
    }

    @Test
    fun `startBackgroundSnatch成功传递会话与目标配置`() = runTest {
        every { servicePort.isSupported } returns true
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
            JwxtSelectionOperResult.Fail("人数已满")
        vm.addTargetWithCheck(selectionCourse())
        advanceMain()
        var captured: SnatchServiceConfig? = null
        every { servicePort.start(any()) } calls { (config: SnatchServiceConfig) ->
            captured = config
            true
        }

        val ok = vm.startBackgroundSnatch()

        assertTrue(ok)
        assertEquals("r1", captured?.rotationId)
        assertEquals("st-1", captured?.sessionTime)
        assertEquals(1, captured?.targets?.size)
        assertEquals("k1|n1|01", captured?.targets?.first()?.key)
    }

    @Test
    fun `startBackgroundSnatch无目标返回false`() = runTest {
        every { servicePort.isSupported } returns true
        val vm = makeViewModel()
        enterSession(vm)

        val ok = vm.startBackgroundSnatch()

        assertFalse(ok)
        assertEquals("请先添加抢课目标", vm.uiState.value.error)
    }

    @Test
    fun `stopBackgroundSnatch调用port停止`() = runTest {
        every { servicePort.isSupported } returns true
        val vm = makeViewModel()

        vm.stopBackgroundSnatch()

        verify(VerifyMode.exactly(1)) { servicePort.stop() }
    }

    // endregion

    // region 退课

    @Test
    fun `dropSelectedCourse成功从列表移除`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        assertEquals(2, vm.uiState.value.selectedCourses.size)
        everySuspend { gateway.dropSelection(any(), any(), any(), any()) } returns okResponse()

        val toDrop = JwxtSelectedCourse(courseName = "体育", noticeId = "n9")
        vm.dropSelectedCourse(toDrop)
        advanceMain()

        assertEquals(1, vm.uiState.value.selectedCourses.size)
        assertEquals("n8", vm.uiState.value.selectedCourses[0].noticeId)
        assertTrue(vm.uiState.value.logs.any { it.message.contains("退课成功") })
    }

    @Test
    fun `dropSelectedCourse失败保留列表`() = runTest {
        val vm = makeViewModel()
        enterSession(vm)
        everySuspend { gateway.dropSelection(any(), any(), any(), any()) } returns
            JwxtSelectionResponse(errorCode = "error_3", errorMessage = "不在退课时间内")

        val toDrop = JwxtSelectedCourse(courseName = "体育", noticeId = "n9")
        vm.dropSelectedCourse(toDrop)
        advanceMain()

        assertEquals(2, vm.uiState.value.selectedCourses.size)
        assertTrue(vm.uiState.value.logs.any { it.message.contains("退课失败") && it.message.contains("不在退课时间内") })
    }

    // endregion

    // region loadSelectedCourses

    @Test
    fun `loadSelectedCourses失败仅复位加载状态`() = runTest {
        val vm = makeViewModel()
        stubSessionInit("r1")
        everySuspend { gateway.fetchSelectedCourses(any()) } throws RuntimeException("网络故障")

        vm.selectRotation("r1")
        advanceMain()

        // 自动加载已选课程失败被吞掉：列表为空且无致命错误
        assertTrue(vm.uiState.value.selectedCourses.isEmpty())
        assertTrue(vm.uiState.value.sessionReady)
    }

    // endregion
}
