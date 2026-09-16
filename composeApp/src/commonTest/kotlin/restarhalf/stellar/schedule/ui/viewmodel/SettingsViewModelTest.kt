package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.usecase.CancelAllCourseRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.CancelAllExamRemindersUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextCourseReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.ScheduleNextExamReminderUseCase
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * SettingsViewModel 单元测试。
 *
 * 全部依赖为接口（可直接 mock）+ final class UseCase 走真实实例：
 * - CancelAllCourseRemindersUseCase(courseReminder)
 * - CancelAllExamRemindersUseCase(examReminder)
 * - ScheduleNextCourseReminderUseCase(courseRepository, courseReminder)
 * - ScheduleNextExamReminderUseCase(FetchExaminationsUseCase(...), examReminder)
 * - FetchSemesterIdsUseCase(authWorkflow, academic, settings)
 * - VerifyGitHubStarUseCase(papersPort, settings)
 *
 * 关键点：
 * 1. uiState 是 stateIn(WhileSubscribed) —— 先 subscribeUi 驱动多层 combine；
 * 2. 设置 observe 流用类级 MutableStateFlow stub，测试中改值驱动 uiState；
 * 3. 取消提醒走 withContext(Dispatchers.IO) 真实跨线程 —— delay + advanceMain 后再 verify。
 */
class SettingsViewModelTest {

    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val courseReminder = mock<CourseReminderPort>(MockMode.autofill)
    private val examReminder = mock<ExamReminderPort>(MockMode.autofill)
    private val examRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val papersPort = mock<PapersPort>(MockMode.autofill)

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    // 设置 observe 流（类级可变，测试中改值驱动 uiState）
    private val showFlow = MutableStateFlow(true)
    private val reminderFlow = MutableStateFlow(false)
    private val examReminderFlow = MutableStateFlow(false)
    private val themeFlow = MutableStateFlow(0)
    private val floatingBarFlow = MutableStateFlow(0)
    private val termFlow = MutableStateFlow("")
    private val logFlow = MutableStateFlow(false)
    private val rowHeightFlow = MutableStateFlow(56)
    private val tokenFlow = MutableStateFlow("")
    private val profileFlow = MutableStateFlow(JwxtAuthProfile())

    private fun makeViewModel(): SettingsViewModel {
        every { settings.observeShowNonCurrentWeek() } returns showFlow
        every { settings.observeCourseReminderEnabled() } returns reminderFlow
        every { settings.observeExamReminderEnabled() } returns examReminderFlow
        every { settings.observeThemeMode() } returns themeFlow
        every { settings.observeFloatingBar() } returns floatingBarFlow
        every { settings.observeSelectedTerm() } returns termFlow
        every { settings.observeLogEnabled() } returns logFlow
        every { settings.observeScheduleRowHeight() } returns rowHeightFlow
        every { settings.observeCachedSemesterIds() } returns flowOf(emptyList())
        every { settings.getStarVerified() } returns false
        every { auth.observeToken() } returns tokenFlow
        every { auth.observeProfile() } returns profileFlow
        everySuspend { authWorkflow.ensureLoggedIn() } returns Unit
        everySuspend { academic.fetchExaminations(any(), any()) } returns emptyList()
        return SettingsViewModel(
            auth = auth,
            authWorkflow = authWorkflow,
            settings = settings,
            cancelAllCourseReminders = CancelAllCourseRemindersUseCase(courseReminder),
            cancelAllExamReminders = CancelAllExamRemindersUseCase(examReminder),
            fetchSemesterIds = FetchSemesterIdsUseCase(authWorkflow, academic, settings),
            scheduleNextCourseReminder = ScheduleNextCourseReminderUseCase(
                courseRepository, courseReminder,
            ),
            scheduleNextExamReminder = ScheduleNextExamReminderUseCase(
                FetchExaminationsUseCase(authWorkflow, academic, examRepository, auth, settings),
                examReminder,
            ),
            verifyGitHubStar = VerifyGitHubStarUseCase(papersPort, settings),
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
    private fun TestScope.subscribeUi(vm: SettingsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    /** 轮询等待 uiState 满足谓词，每轮主动 drain Main 队列 */
    private suspend fun awaitState(
        vm: SettingsViewModel,
        timeoutMs: Long = 5000,
        predicate: (SettingsViewModel.SettingsUiState) -> Boolean,
    ): SettingsViewModel.SettingsUiState {
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
    fun `uiState反映偏好与学期设置`() = runTest {
        themeFlow.value = 1
        rowHeightFlow.value = 80
        termFlow.value = "2025-1"
        logFlow.value = true
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = awaitState(vm) { it.themeMode == 1 }

        assertEquals(80, state.scheduleRowHeight)
        assertEquals("2025-1", state.selectedTerm)
        assertEquals(true, state.logEnabled)
        assertEquals(true, state.showNonCurrentWeek)
    }

    @Test
    fun `uiState反映认证状态`() = runTest {
        tokenFlow.value = "tok"
        profileFlow.value = JwxtAuthProfile(name = "小明", userNo = "2023001")
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = awaitState(vm) { it.authToken == "tok" }

        assertEquals("小明", state.profile.name)
        assertEquals("2023001", state.profile.userNo)
    }

    @Test
    fun `onThemeModeChanged透传`() = runTest {
        val vm = makeViewModel()

        vm.onThemeModeChanged(2)

        verify(VerifyMode.exactly(1)) { settings.setThemeMode(2) }
    }

    @Test
    fun `onFloatingBarChanged透传`() = runTest {
        val vm = makeViewModel()

        vm.onFloatingBarChanged(1)

        verify(VerifyMode.exactly(1)) { settings.setFloatingBar(1) }
    }

    @Test
    fun `onShowNonCurrentWeekChanged透传`() = runTest {
        val vm = makeViewModel()

        vm.onShowNonCurrentWeekChanged(false)

        verify(VerifyMode.exactly(1)) { settings.setShowNonCurrentWeek(false) }
    }

    @Test
    fun `onScheduleRowHeightChanged透传`() = runTest {
        val vm = makeViewModel()

        vm.onScheduleRowHeightChanged(72)

        verify(VerifyMode.exactly(1)) { settings.setScheduleRowHeight(72) }
    }

    @Test
    fun `onLogEnabledChanged透传`() = runTest {
        val vm = makeViewModel()

        vm.onLogEnabledChanged(true)

        verify(VerifyMode.exactly(1)) { settings.setLogEnabled(true) }
    }

    @Test
    fun `开启课程提醒只写设置不取消提醒`() = runTest {
        val vm = makeViewModel()

        vm.onReminderEnabledChanged(true)
        advanceMain()

        verify(VerifyMode.exactly(1)) { settings.setCourseReminderEnabled(true) }
        verify(VerifyMode.not) { courseReminder.cancelAll() }
    }

    @Test
    fun `关闭课程提醒并取消课程提醒`() = runTest {
        val vm = makeViewModel()

        vm.onReminderEnabledChanged(false)
        // withContext(Dispatchers.IO) 真实跨线程，等待完成后再 drain
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        verify(VerifyMode.exactly(1)) { settings.setCourseReminderEnabled(false) }
        verify(VerifyMode.exactly(1)) { courseReminder.cancelAll() }
    }

    @Test
    fun `开启考试提醒只写设置不取消提醒`() = runTest {
        val vm = makeViewModel()

        vm.onExamReminderEnabledChanged(true)
        advanceMain()

        verify(VerifyMode.exactly(1)) { settings.setExamReminderEnabled(true) }
        verify(VerifyMode.not) { examReminder.cancelAll() }
    }

    @Test
    fun `关闭考试提醒并取消考试提醒`() = runTest {
        val vm = makeViewModel()

        vm.onExamReminderEnabledChanged(false)
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        verify(VerifyMode.exactly(1)) { settings.setExamReminderEnabled(false) }
        verify(VerifyMode.exactly(1)) { examReminder.cancelAll() }
    }

    @Test
    fun `onSelectedTermChanged写学期并调度考试提醒`() = runTest {
        val vm = makeViewModel()

        vm.onSelectedTermChanged("2025-2")
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        verify(VerifyMode.exactly(1)) { settings.setSelectedTerm("2025-2") }
        verify(VerifyMode.exactly(1)) { settings.setActiveScheduleTerm("2025-2") }
        verify(VerifyMode.exactly(1)) { examReminder.scheduleNextReminder(any()) }
    }

    @Test
    fun `refreshRemoteTerms有token时拉取填充`() = runTest {
        tokenFlow.value = "tok"
        everySuspend { academic.fetchSemesterIds() } returns listOf("2026-1", "2025-2")
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.refreshRemoteTerms()

        val state = awaitState(vm) { it.remoteTermItems.isNotEmpty() }
        assertEquals(listOf("2026-1", "2025-2"), state.remoteTermItems)
    }

    @Test
    fun `refreshRemoteTerms无token时清空列表`() = runTest {
        tokenFlow.value = "tok"
        everySuspend { academic.fetchSemesterIds() } returns listOf("2026-1")
        val vm = makeViewModel()
        subscribeUi(vm)
        vm.refreshRemoteTerms()
        awaitState(vm) { it.remoteTermItems.isNotEmpty() }

        tokenFlow.value = ""
        vm.refreshRemoteTerms()

        val state = awaitState(vm) { it.remoteTermItems.isEmpty() }
        assertEquals(0, state.remoteTermItems.size)
    }

    @Test
    fun `buildTermSelectionUi无token仅当前学期`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildTermSelectionUi(authToken = "", remoteTermItems = listOf("2025-1"), selectedTerm = "")

        assertEquals(listOf("当前学期"), ui.items)
        assertEquals(0, ui.selectedIndex)
    }

    @Test
    fun `buildTermSelectionUi有token拼接远程学期`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildTermSelectionUi(
            authToken = "tok",
            remoteTermItems = listOf("2026-1", "2025-2"),
            selectedTerm = "2025-2",
        )

        assertEquals(listOf("当前学期", "2026-1", "2025-2"), ui.items)
        assertEquals(2, ui.selectedIndex)
    }

    @Test
    fun `buildTermSelectionUi选中项不在列表时回退0`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildTermSelectionUi(
            authToken = "tok",
            remoteTermItems = listOf("2026-1"),
            selectedTerm = "不存在的学期",
        )

        assertEquals(0, ui.selectedIndex)
    }

    @Test
    fun `buildAccountUi已登录展示姓名与学号班级`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildAccountUi(
            authToken = "tok",
            profile = JwxtAuthProfile(name = "小明", userNo = "2023001", clsName = "软件2301", academyName = "软件学院"),
        )

        assertEquals(true, ui.loggedIn)
        assertEquals("小明", ui.title)
        assertEquals(true, ui.summary.contains("2023001"))
        assertEquals(true, ui.summary.contains("软件2301"))
        assertEquals(true, ui.summary.contains("软件学院"))
    }

    @Test
    fun `buildAccountUi未登录回退文案`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildAccountUi(authToken = "", profile = JwxtAuthProfile())

        assertEquals(false, ui.loggedIn)
        assertEquals("已登录", ui.title)
        assertEquals("用于获取课表", ui.summary)
    }

    @Test
    fun `buildScreenUi校区索引与学期开始摘要`() = runTest {
        val vm = makeViewModel()
        val termStartMs = Instant.parse("2026-01-15T12:00:00Z").toEpochMilliseconds()

        val dev = vm.buildScreenUi(SyncUiState.Idle, Campus.Development, termStartMs)
        val js = vm.buildScreenUi(SyncUiState.Idle, Campus.Jinshitan, termStartMs)

        assertEquals(0, dev.campusSelectedIndex)
        assertEquals(1, js.campusSelectedIndex)
        assertEquals("2026/01/15", dev.termStartSummary)
        assertEquals("立即从教务拉取最新课表", dev.syncSummary)
    }

    @Test
    fun `buildScreenUi同步摘要四态`() = runTest {
        val vm = makeViewModel()

        assertEquals("同步中...", vm.buildScreenUi(SyncUiState.Loading, Campus.Development, 0L).syncSummary)
        assertEquals(
            "同步成功：3 门（开发区校区）",
            vm.buildScreenUi(SyncUiState.Success(3, "开发区校区"), Campus.Development, 0L).syncSummary,
        )
        assertEquals("网络异常", vm.buildScreenUi(SyncUiState.Error("网络异常"), Campus.Development, 0L).syncSummary)
    }

    @Test
    fun `campusFromIndex与selectedTermValueFromIndex`() = runTest {
        val vm = makeViewModel()

        assertEquals(Campus.Development, vm.campusFromIndex(0))
        assertEquals(Campus.Jinshitan, vm.campusFromIndex(1))
        assertEquals("", vm.selectedTermValueFromIndex(listOf("当前学期", "2025-1"), 0))
        assertEquals("2025-1", vm.selectedTermValueFromIndex(listOf("当前学期", "2025-1"), 1))
        assertEquals("", vm.selectedTermValueFromIndex(listOf("当前学期"), 9))
    }

    @Test
    fun `loadPersonalInfo填充并标记hasPersonalInfo`() = runTest {
        val vm = makeViewModel()
        every { settings.getUserAvatarUri() } returns "content://avatar"
        every { settings.getUserNickname() } returns null

        vm.loadPersonalInfo()
        advanceMain()

        assertEquals("content://avatar", vm.personalInfoUiState.value.avatarUri)
        assertEquals(null, vm.personalInfoUiState.value.nickname)
        assertEquals(true, vm.personalInfoUiState.value.hasPersonalInfo)
    }

    @Test
    fun `loadPersonalInfo全空时hasPersonalInfo为false`() = runTest {
        val vm = makeViewModel()
        every { settings.getUserAvatarUri() } returns null
        every { settings.getUserNickname() } returns null

        vm.loadPersonalInfo()
        advanceMain()

        assertEquals(false, vm.personalInfoUiState.value.hasPersonalInfo)
    }

    @Test
    fun `triggerSync执行block并吞异常`() = runTest {
        val vm = makeViewModel()
        var calls = 0

        vm.triggerSync { calls++ }
        vm.triggerSync { throw RuntimeException("同步失败") }
        advanceMain()

        assertEquals(1, calls)
    }
}
