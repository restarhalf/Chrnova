package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import restarhalf.stellar.schedule.domain.usecase.ObserveAllGradesUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * GradeViewModel 单元测试。
 *
 * ObserveAllGradesUseCase 是 final class，用真实实例 + mock GradeRepository/JwxtAuthPort。
 * auth.observeProfile 返回空学号 → UseCase 走 observeAllGrades() 全量分支。
 *
 * 关键点（stateIn(WhileSubscribed) 型 ViewModel）：
 * 1. uiState 必须先订阅才能驱动 combine 上游（subscribeUi）；
 * 2. Main dispatcher 类级安装（@BeforeTest setMain），advanceMain 主动 drain。
 */
class GradeViewModelTest {

    private val gradeRepository = mock<GradeRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    private val profileFlow = MutableStateFlow(JwxtAuthProfile(userNo = ""))
    private val gradesFlow = MutableStateFlow<List<GradeCourse>>(emptyList())
    private val termFlow = MutableStateFlow("")

    private fun makeViewModel(): GradeViewModel {
        every { auth.observeProfile() } returns profileFlow
        every { gradeRepository.observeAllGrades() } returns gradesFlow
        every { settings.observeSelectedTerm() } returns termFlow
        return GradeViewModel(
            observeAllGrades = ObserveAllGradesUseCase(gradeRepository, auth),
            settings = settings,
        )
    }

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 驱动 stateIn(WhileSubscribed)：订阅 uiState，combine 上游才开始运行 */
    private fun TestScope.subscribeUi(vm: GradeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    /** 轮询等待 uiState 满足谓词，每轮主动 drain Main 队列 */
    private suspend fun awaitState(
        vm: GradeViewModel,
        timeoutMs: Long = 5000,
        predicate: (GradeViewModel.GradeUiState) -> Boolean,
    ): GradeViewModel.GradeUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        while (Clock.System.now().toEpochMilliseconds() < deadline) {
            advanceMain()
            val state = vm.uiState.value
            if (predicate(state)) return state
            withContext(Dispatchers.Default) { delay(10) }
        }
        error("等待 uiState 超时，当前: ${vm.uiState.value}")
    }

    private fun grade(
        semester: String,
        gradeId: String = "g-$semester",
        courseName: String = "课程-$semester",
    ) = GradeCourse(gradeId = gradeId, courseName = courseName, semester = semester)

    @Test
    fun `本地有当前学期数据时优先展示当前学期`() = runTest {
        termFlow.value = "2024-2025-1"
        gradesFlow.value = listOf(grade("2024-2025-1"), grade("2023-2024-1"))
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = awaitState(vm) { it.report.achievements.isNotEmpty() }

        assertEquals(1, state.report.achievements.size)
        assertEquals("2024-2025-1", state.report.achievements[0].semester)
    }

    @Test
    fun `当前学期无本地数据时回退展示summary`() = runTest {
        termFlow.value = "2024-2025-1"
        gradesFlow.value = emptyList()
        val vm = makeViewModel()
        subscribeUi(vm)
        vm.bindLoader {
            TermGradeReport(studentName = "网络", achievements = listOf(grade("网络学期")), earnedCredits = "120")
        }
        vm.load()

        val state = awaitState(vm) { it.report.earnedCredits == "120" }

        assertEquals(1, state.report.achievements.size)
        assertEquals("网络学期", state.report.achievements[0].semester)
    }

    @Test
    fun `当前学期与summary都无数据时回退最新学期`() = runTest {
        termFlow.value = ""
        gradesFlow.value = listOf(grade("2023-2024-1"), grade("2024-2025-1"))
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = awaitState(vm) { it.report.achievements.isNotEmpty() }

        assertTrue(state.report.achievements.all { it.semester == "2024-2025-1" })
    }

    @Test
    fun `彻底无数据时report为空`() = runTest {
        termFlow.value = "2024-2025-1"
        gradesFlow.value = emptyList()
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = awaitState(vm) { true }

        assertEquals(0, state.report.achievements.size)
        assertFalse(state.loading)
    }

    @Test
    fun `load成功更新summary并复位loading`() = runTest {
        termFlow.value = "9999"
        gradesFlow.value = emptyList()
        val vm = makeViewModel()
        subscribeUi(vm)
        vm.bindLoader { TermGradeReport(earnedCredits = "120") }

        vm.load()

        val state = awaitState(vm) { it.report.earnedCredits == "120" && !it.loading }
        assertFalse(state.loading)
    }

    @Test
    fun `load失败置error并保留旧summary`() = runTest {
        termFlow.value = "9999"
        gradesFlow.value = emptyList()
        val vm = makeViewModel()
        subscribeUi(vm)
        vm.bindLoader { TermGradeReport(earnedCredits = "120") }
        vm.load()
        awaitState(vm) { it.report.earnedCredits == "120" }

        vm.bindLoader { throw RuntimeException("网络超时") }
        vm.load()

        val state = awaitState(vm) { it.error.isNotBlank() }
        assertFalse(state.loading)
        assertEquals("120", state.report.earnedCredits)
    }

    @Test
    fun `loading期间重复load不触发第二次`() = runTest {
        termFlow.value = "9999"
        gradesFlow.value = emptyList()
        val vm = makeViewModel()
        subscribeUi(vm)
        val gate = CompletableDeferred<TermGradeReport>()
        var calls = 0
        vm.bindLoader {
            calls++
            gate.await()
        }

        vm.load()
        vm.load()

        assertEquals(1, calls)
        gate.complete(TermGradeReport())
        advanceMain()
        assertEquals(1, calls)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `未绑定loader时load为no-op`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.load()
        advanceMain()

        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `buildScreenUi错误优先于空状态`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(report = TermGradeReport(), loading = false, error = "加载失败")

        assertEquals("加载失败", ui.statusText)
    }

    @Test
    fun `buildScreenUi空数据提示暂无成绩数据`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(report = TermGradeReport(), loading = false, error = "")

        assertEquals("暂无成绩数据", ui.statusText)
    }

    @Test
    fun `buildScreenUi加载中无statusText`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(report = TermGradeReport(), loading = true, error = "")

        assertEquals(null, ui.statusText)
    }

    @Test
    fun `buildScreenUi构建卡片字段`() = runTest {
        val vm = makeViewModel()
        val report = TermGradeReport(
            achievements = listOf(
                GradeCourse(gradeId = "g1", courseCode = "CS101", courseName = "", score = "59", credit = 3.0)
            )
        )

        val ui = vm.buildScreenUi(report, loading = false, error = "")

        assertEquals(1, ui.cards.size)
        val card = ui.cards[0]
        assertEquals("未命名课程", card.title)
        assertEquals("59", card.scoreText)
        assertTrue(card.jdText.contains("绩点"))
        assertTrue(card.isFailed)
        assertTrue(card.subtitle.contains("CS101"))
        assertTrue(card.subtitle.contains("学分:3"))
    }

    @Test
    fun `重复idKey自动加后缀防撞`() = runTest {
        val vm = makeViewModel()
        val report = TermGradeReport(
            achievements = listOf(
                GradeCourse(gradeId = "g1", courseName = "A"),
                GradeCourse(gradeId = "g1", courseName = "B"),
            )
        )

        val ui = vm.buildScreenUi(report, loading = false, error = "")

        assertEquals("g1", ui.cards[0].idKey)
        assertEquals("g1#1", ui.cards[1].idKey)
    }

    @Test
    fun `buildGradeDetailsSummary包含关键行`() = runTest {
        val vm = makeViewModel()

        val summary = vm.buildGradeDetailsSummary(
            GradeCourse(courseCode = "CS101", score = "88", gradePoint = 3.5)
        )

        assertTrue(summary.contains("课程号：CS101"))
        assertTrue(summary.contains("成绩：88"))
    }

    @Test
    fun `buildGradeDetailsSummary空字段回退暂无`() = runTest {
        val vm = makeViewModel()

        val summary = vm.buildGradeDetailsSummary(GradeCourse())

        assertTrue(summary.contains("课程号：暂无"))
        assertTrue(summary.contains("成绩：--"))
    }
}
