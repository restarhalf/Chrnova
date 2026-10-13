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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.RemoteCampus
import restarhalf.stellar.schedule.domain.model.SyncResult
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.SyncPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import restarhalf.stellar.schedule.domain.usecase.BindUnboundDataUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesSimpleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.JwxtLoginUseCase
import restarhalf.stellar.schedule.domain.usecase.RunSyncUseCase
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class AppViewModelTest {

    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val sync = mock<SyncPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val examRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val gradeRepository = mock<GradeRepository>(MockMode.autofill)
    private val reminderScheduler = mock<ReminderSchedulerPort>(MockMode.autofill)

    private val campusFlow = MutableStateFlow(Campus.Jinshitan)
    private val termStartFlow = MutableStateFlow(1000L)
    private val totalWeeksFlow = MutableStateFlow(20)
    private val logFlow = MutableStateFlow(false)
    private val selectedTermFlow = MutableStateFlow("2026-1")
    private val profileFlow = MutableStateFlow(JwxtAuthProfile())

    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val jinshitan = RemoteCampus(id = "c-js", name = "金石滩校区", isDefault = false)
    private val development = RemoteCampus(id = "c-kf", name = "开发区校区", isDefault = false)

    private fun syncResult(inserted: Int, campusId: String) = SyncResult(
        inserted = inserted,
        semesterId = "2026-1",
        campusId = campusId,
        campusName = "",
        week = "all",
    )

    private fun stubHappyPath() {
        every { settings.observeSelectedTerm() } returns selectedTermFlow
        every { timetable.getCampus() } returns Campus.Jinshitan
        everySuspend { academic.fetchCampuses() } returns listOf(jinshitan, development)
        everySuspend { sync.sync(any(), any(), any()) } returns syncResult(5, "c-js")
        everySuspend { sync.fetchTermStartDate(any(), any()) } returns 1000L
        everySuspend { academic.fetchTeachingWeekTotal() } returns 20
    }

    private fun makeViewModel(): AppViewModel {
        every { timetable.observeCampus() } returns campusFlow
        every { timetable.observeTermStartMs() } returns termStartFlow
        every { timetable.observeTotalWeeks() } returns totalWeeksFlow
        every { timetable.getCampus() } returns Campus.Jinshitan
        every { timetable.getTermStartMs() } returns 1000L
        every { timetable.getTotalWeeks() } returns 20
        every { settings.observeLogEnabled() } returns logFlow
        every { settings.observeSelectedTerm() } returns selectedTermFlow
        every { auth.observeProfile() } returns profileFlow
        return AppViewModel(
            auth = auth,
            timetable = timetable,
            settings = settings,
            fetchExaminations = FetchExaminationsSimpleUseCase(
                FetchExaminationsUseCase(authWorkflow, academic, examRepository, auth, settings),
            ),
            fetchGrades = FetchGradesSimpleUseCase(
                FetchGradesUseCase(authWorkflow, academic, settings, gradeRepository, auth),
            ),
            jwxtLoginUseCase = JwxtLoginUseCase(authWorkflow),
            runSyncUseCase = RunSyncUseCase(
                authWorkflow = authWorkflow,
                academic = academic,
                timetable = timetable,
                settings = settings,
                sync = sync,
                reminderScheduler = reminderScheduler,
            ),
            bindUnboundData = BindUnboundDataUseCase(auth, courseRepository, examRepository, academic),
        )
    }

    private fun TestScope.subscribeUi(vm: AppViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    private suspend fun awaitState(
        vm: AppViewModel,
        timeoutMs: Long = 5000,
        predicate: (AppViewModel.AppUiState) -> Boolean,
    ): AppViewModel.AppUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        while (Clock.System.now().toEpochMilliseconds() < deadline) {
            advanceMain()
            val state = vm.uiState.value
            if (predicate(state)) return state
            withContext(Dispatchers.Default) { delay(10) }
        }
        error("等待 uiState 超时，当前: ${vm.uiState.value}")
    }



    @Test
    fun `init未登录时bindUnboundData静默完成`() = runTest {
        val vm = makeViewModel()
        advanceMain()


        verifySuspend(VerifyMode.not) { courseRepository.bindUnboundCourses(any()) }
        verifySuspend(VerifyMode.not) { examRepository.bindUnboundExaminations(any()) }
    }





    @Test
    fun `uiState初值来自timetable同步方法`() = runTest {
        val vm = makeViewModel()


        val state = vm.uiState.value
        assertEquals(Campus.Jinshitan, state.campus)
        assertEquals(1000L, state.termStartMs)
        assertEquals(20, state.totalWeeks)
    }

    @Test
    fun `uiState响应校区流变化`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        campusFlow.value = Campus.Development

        val state = awaitState(vm) { it.campus == Campus.Development }
        assertEquals(Campus.Development, state.campus)
    }

    @Test
    fun `uiState响应学期开始与总周数变化`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        termStartFlow.value = 2000L
        totalWeeksFlow.value = 16

        val state = awaitState(vm) { it.termStartMs == 2000L }
        assertEquals(16, state.totalWeeks)
    }





    @Test
    fun `runSync成功携带插入数与校区名`() = runTest {
        stubHappyPath()
        val vm = makeViewModel()
        advanceMain()

        vm.runSync()

        assertEquals(SyncUiState.Success(inserted = 5, campusName = "金石滩校区"), vm.syncUiState.value)
    }

    @Test
    fun `runSync失败置Error且文案非空`() = runTest {
        every { settings.observeSelectedTerm() } returns selectedTermFlow
        every { timetable.getCampus() } returns Campus.Jinshitan
        everySuspend { academic.fetchCampuses() } throws RuntimeException("网络故障")
        val vm = makeViewModel()
        advanceMain()

        vm.runSync()

        val state = vm.syncUiState.value
        assertTrue(state is SyncUiState.Error)
        assertNotEquals("", state.message)
    }

    @Test
    fun `acknowledgeSyncResult把Error复位为Idle`() = runTest {
        every { settings.observeSelectedTerm() } returns selectedTermFlow
        every { timetable.getCampus() } returns Campus.Jinshitan
        everySuspend { academic.fetchCampuses() } throws RuntimeException("网络故障")
        val vm = makeViewModel()
        advanceMain()

        vm.runSync()
        assertTrue(vm.syncUiState.value is SyncUiState.Error)

        vm.acknowledgeSyncResult()

        assertEquals(SyncUiState.Idle, vm.syncUiState.value)
    }

    @Test
    fun `acknowledgeSyncResult对Success保持不变`() = runTest {
        stubHappyPath()
        val vm = makeViewModel()
        advanceMain()
        vm.runSync()
        assertEquals(SyncUiState.Success(5, "金石滩校区"), vm.syncUiState.value)

        vm.acknowledgeSyncResult()

        assertEquals(SyncUiState.Success(5, "金石滩校区"), vm.syncUiState.value)
    }





    @Test
    fun `logout清除认证信息`() = runTest {
        val vm = makeViewModel()

        vm.logout()

        verify(VerifyMode.exactly(1)) { auth.clear() }
    }

    @Test
    fun `onCampusChanged写timetable`() = runTest {
        val vm = makeViewModel()
        advanceMain()

        vm.onCampusChanged(Campus.Development)
        advanceMain()

        verify(VerifyMode.exactly(1)) { timetable.setCampus(Campus.Development) }
    }

    @Test
    fun `onTermStartMsChanged写timetable`() = runTest {
        val vm = makeViewModel()

        vm.onTermStartMsChanged(3000L)

        verify(VerifyMode.exactly(1)) { timetable.setTermStartMs(3000L) }
    }

    @Test
    fun `onTotalWeeksChanged写timetable`() = runTest {
        val vm = makeViewModel()

        vm.onTotalWeeksChanged(18)

        verify(VerifyMode.exactly(1)) { timetable.setTotalWeeks(18) }
    }





    @Test
    fun `login透传参数到authWorkflow`() = runTest {
        val vm = makeViewModel()
        everySuspend { authWorkflow.login(any(), any(), any(), any(), any()) } returns Unit

        vm.login(
            userNo = "2023001",
            password = "pwd",
            captchaData = "cap",
            codeVal = "1234",
            p = "pp",
        )

        verifySuspend(VerifyMode.exactly(1)) {
            authWorkflow.login(
                userNo = "2023001",
                password = "pwd",
                captchaData = "cap",
                codeVal = "1234",
                p = "pp",
            )
        }
    }

    @Test
    fun `login失败向上抛出异常`() = runTest {
        val vm = makeViewModel()
        everySuspend { authWorkflow.login(any(), any(), any(), any(), any()) } throws
            RuntimeException("验证码错误")

        assertFailsWith<RuntimeException> {
            vm.login(userNo = "2023001", password = "pwd")
        }
    }


}
