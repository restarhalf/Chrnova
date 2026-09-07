package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import restarhalf.stellar.schedule.domain.usecase.CalculateElectiveCreditsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * ElectiveCreditViewModel 单元测试。
 *
 * FetchSemesterIdsUseCase / CalculateElectiveCreditsUseCase 为 final class，
 * 用真实实现 + mock 端口（AcademicPort / SettingsPort / GradeRepository）。
 * load() 协程经 viewModelScope(Main) 与真实 IO 调度，断言用真实时间轮询 +
 * drain Main 队列（advanceUntilIdle），避免非可取消恢复入队后停滞。
 */
class ElectiveCreditViewModelTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val gradeRepository = mock<GradeRepository>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun makeViewModel() = ElectiveCreditViewModel(
        authWorkflow = authWorkflow,
        academic = academic,
        auth = auth,
        gradeRepository = gradeRepository,
        fetchSemesterIds = FetchSemesterIdsUseCase(authWorkflow, academic, settings),
        calculateElectiveCredits = CalculateElectiveCreditsUseCase(),
    )

    private fun stubSemesters(ids: List<String>) {
        everySuspend { academic.fetchSemesterIds() } returns ids
    }

    private fun grade(
        code: String = "X1A001",
        semester: String = "2026-1",
        credit: Double = 2.0,
    ) = GradeCourse(
        courseCode = code,
        courseName = "选修课",
        score = "90",
        credit = credit,
        semester = semester,
    )

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    private suspend fun awaitState(
        vm: ElectiveCreditViewModel,
        timeoutMs: Long = 5_000,
        predicate: (ElectiveCreditViewModel.ElectiveCreditUiState) -> Boolean,
    ): ElectiveCreditViewModel.ElectiveCreditUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        var state = vm.uiState.value
        while (!predicate(state) && Clock.System.now().toEpochMilliseconds() < deadline) {
            advanceMain()
            withContext(Dispatchers.Default) { delay(10) }
            state = vm.uiState.value
        }
        return state
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load成功时按学期聚合成绩并计算分类学分`() = runTest {
        stubSemesters(listOf("2026-1", "2025-2"))
        everySuspend { academic.fetchCurrentTermId() } returns "2026-1"
        everySuspend { academic.fetchGradeReport(any()) } returns
            TermGradeReport(achievements = listOf(grade()))
        everySuspend { academic.fetchGuidanceTeachingCourses(any(), any(), any()) } returns emptyList()
        val vm = makeViewModel()

        vm.load()
        val state = awaitState(vm) { !it.loading }

        assertEquals("", state.error)
        assertTrue(state.categories.isNotEmpty(), "应计算出学分分类")
        verifySuspend(VerifyMode.exactly(1)) { academic.fetchGradeReport("2026-1") }
        verifySuspend(VerifyMode.exactly(1)) { academic.fetchGradeReport("2025-2") }
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.ensureLoggedIn() }
    }

    @Test
    fun `load时当前学期为空置错误`() = runTest {
        everySuspend { academic.fetchCurrentTermId() } returns ""
        val vm = makeViewModel()

        vm.load()
        val state = awaitState(vm) { !it.loading }

        assertEquals("无法获取当前学期", state.error)
        assertTrue(state.categories.isEmpty())
        verifySuspend(VerifyMode.not) { academic.fetchGradeReport(any()) }
    }

    @Test
    fun `load时学期列表为空置错误`() = runTest {
        everySuspend { academic.fetchCurrentTermId() } returns "2026-1"
        stubSemesters(emptyList())
        every { settings.observeCachedSemesterIds() } returns flowOf(emptyList())
        val vm = makeViewModel()

        vm.load()
        val state = awaitState(vm) { !it.loading }

        assertEquals("暂无学期数据", state.error)
    }

    @Test
    fun `网络成绩失败时回退本地按学号过滤`() = runTest {
        stubSemesters(listOf("2026-1"))
        everySuspend { academic.fetchCurrentTermId() } returns "2026-1"
        everySuspend { academic.fetchGradeReport(any()) } throws RuntimeException("connection timeout")
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        everySuspend { gradeRepository.getAllGradesByUserNo("2023001") } returns listOf(
            grade(code = "X2B002", credit = 1.5),
            grade(code = "X2B003", semester = "2025-2", credit = 1.0), // 其他学期，应被过滤
        )
        everySuspend { academic.fetchGuidanceTeachingCourses(any(), any(), any()) } returns emptyList()
        val vm = makeViewModel()

        vm.load()
        val state = awaitState(vm) { !it.loading }

        assertEquals("", state.error)
        val x2 = state.categories.firstOrNull { it.code == "X2" }
        assertTrue(x2 != null && x2.courses.isNotEmpty(), "X2 分类应包含本地回退课程")
        assertEquals(1.5, x2.credits)
    }

    @Test
    fun `load异常时置用户可读错误`() = runTest {
        everySuspend { academic.fetchCurrentTermId() } throws RuntimeException("no session")
        val vm = makeViewModel()

        vm.load()
        val state = awaitState(vm) { !it.loading }

        assertTrue(state.error.isNotEmpty(), "异常后应有错误信息")
        assertTrue(state.categories.isEmpty())
    }
}
