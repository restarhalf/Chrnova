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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.SyncExamEventsToCalendarUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * ExaminationViewModel 单元测试。
 *
 * IsExamNotEndedUseCase 无参纯逻辑类直接构造；ObserveAllExaminationsUseCase 与
 * SyncExamEventsToCalendarUseCase 是 final class，用真实实例 + mock 端口
 * （ExaminationRepository/JwxtAuthPort/SettingsPort/CalendarEventPort）。
 *
 * 关键点：
 * 1. uiState 是 stateIn(WhileSubscribed) —— 必须先 subscribeUi 驱动 combine；
 * 2. refreshExamCalendar 内 withContext(AppIoDispatcher=Dispatchers.IO) 真实跨线程，
 *    等待用 delay + advanceMain drain。
 */
class ExaminationViewModelTest {

    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val calendarEvent = mock<CalendarEventPort>(MockMode.autofill)

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    private val profileFlow = MutableStateFlow(JwxtAuthProfile(userNo = ""))
    private val examsFlow = MutableStateFlow<List<Examination>>(emptyList())
    private val termFlow = MutableStateFlow("")

    private fun makeViewModel(): ExaminationViewModel {
        every { auth.observeProfile() } returns profileFlow
        every { examinationRepository.observeAllExaminations() } returns examsFlow
        every { examinationRepository.observeExaminationsByUserNo(any()) } returns examsFlow
        every { settings.observeSelectedTerm() } returns termFlow
        val observeAllExaminations = ObserveAllExaminationsUseCase(examinationRepository, auth)
        return ExaminationViewModel(
            isExamNotEnded = IsExamNotEndedUseCase(),
            observeAllExaminations = observeAllExaminations,
            auth = auth,
            settings = settings,
            syncExamEventsToCalendar = SyncExamEventsToCalendarUseCase(
                observeAllExaminations = observeAllExaminations,
                calendarEvent = calendarEvent,
                settings = settings,
            ),
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
    private fun TestScope.subscribeUi(vm: ExaminationViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    /** 轮询等待 uiState 满足谓词，每轮主动 drain Main 队列 */
    private suspend fun awaitState(
        vm: ExaminationViewModel,
        timeoutMs: Long = 5000,
        predicate: (ExaminationViewModel.ExaminationUiState) -> Boolean,
    ): ExaminationViewModel.ExaminationUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        while (Clock.System.now().toEpochMilliseconds() < deadline) {
            advanceMain()
            val state = vm.uiState.value
            if (predicate(state)) return state
            withContext(Dispatchers.Default) { delay(10) }
        }
        error("等待 uiState 超时，当前: ${vm.uiState.value}")
    }

    private fun exam(
        time: String = "2025-01-08 14:00-16:00",
        semesterId: String = "",
        userNo: String = "",
        courseName: String = "",
    ) = Examination(courseName = courseName, time = time, semesterId = semesterId, userNo = userNo)

    @Test
    fun `uiState按学期过滤`() = runTest {
        termFlow.value = "2024-2025-1"
        examsFlow.value = listOf(
            exam(semesterId = "2024-2025-1", courseName = "A"),
            exam(semesterId = "2023-2024-1", courseName = "B"),
        )
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = awaitState(vm) { it.items.isNotEmpty() }

        assertEquals(1, state.items.size)
        assertEquals("A", state.items[0].courseName)
    }

    @Test
    fun `uiState按userNo过滤`() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "2023001")
        examsFlow.value = listOf(
            exam(userNo = "2023001", courseName = "Mine"),
            exam(userNo = "2023002", courseName = "Others"),
        )
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = awaitState(vm) { it.items.isNotEmpty() }

        assertEquals(1, state.items.size)
        assertEquals("Mine", state.items[0].courseName)
    }

    @Test
    fun `profile与term为空时不过滤`() = runTest {
        profileFlow.value = JwxtAuthProfile(userNo = "")
        termFlow.value = ""
        examsFlow.value = listOf(
            exam(userNo = "2023001", semesterId = "2024-2025-1", courseName = "A"),
            exam(userNo = "2023002", semesterId = "2023-2024-1", courseName = "B"),
        )
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = awaitState(vm) { it.items.isNotEmpty() }

        assertEquals(2, state.items.size)
    }

    @Test
    fun `load失败置error并复位loading`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        vm.bindLoader { throw RuntimeException("服务超时") }

        vm.load()

        val state = awaitState(vm) { it.error.isNotBlank() }
        assertFalse(state.loading)
    }

    @Test
    fun `load成功复位loading并清空error`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        vm.bindLoader { listOf(exam(courseName = "X")) }

        vm.load()

        val state = awaitState(vm) { !it.loading }
        assertEquals("", state.error)
    }

    @Test
    fun `loading期间重复load不触发第二次`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        val gate = CompletableDeferred<List<Examination>>()
        var calls = 0
        vm.bindLoader {
            calls++
            gate.await()
        }

        vm.load()
        vm.load()

        assertEquals(1, calls)
        gate.complete(emptyList())
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
    fun `refreshExamCalendar未开启提醒时不写日历`() = runTest {
        val vm = makeViewModel()
        every { settings.observeExamReminderEnabled() } returns flowOf(false)

        vm.refreshExamCalendar()
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        verifySuspend(VerifyMode.not) { calendarEvent.syncExamEvents(any()) }
    }

    @Test
    fun `refreshExamCalendar开启提醒且有权限时同步日历`() = runTest {
        val vm = makeViewModel()
        every { settings.observeExamReminderEnabled() } returns flowOf(true)
        every { calendarEvent.hasCalendarPermission() } returns true
        everySuspend { calendarEvent.syncExamEvents(any()) } returns CalendarEventPort.SyncResult.Success(1)

        vm.refreshExamCalendar()
        // withContext(Dispatchers.IO) 真实跨线程，等待恢复后再 drain Main 队列
        withContext(Dispatchers.Default) { delay(100) }
        advanceMain()

        verifySuspend(VerifyMode.exactly(1)) { calendarEvent.syncExamEvents(any()) }
    }

    @Test
    fun `buildScreenUi按日期排序`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(
            items = listOf(
                exam(time = "2025-01-10 09:00-11:00", courseName = "晚"),
                exam(time = "2025-01-08 14:00-16:00", courseName = "早"),
            ),
            loading = false,
            error = "",
            nowMs = 0L,
        )

        assertEquals("早", ui.cards[0].title)
        assertEquals("晚", ui.cards[1].title)
    }

    @Test
    fun `buildScreenUi构建卡片字段`() = runTest {
        val vm = makeViewModel()
        val e = Examination(
            id = 1,
            courseNumber = "CS101",
            courseName = "高数",
            time = "2025-01-08 14:00-16:00",
            examinationPlace = "A101",
            zwh = "05",
            ksbz = "正常",
        )

        val ui = vm.buildScreenUi(listOf(e), loading = false, error = "", nowMs = 0L)

        assertEquals(1, ui.cards.size)
        val card = ui.cards[0]
        assertEquals("高数", card.title)
        assertEquals("考试日期:2025-01-08", card.dateText)
        assertTrue(card.timeText.startsWith("考试时间:星期"))
        assertEquals("地点：A101", card.locationText)
        assertEquals("座位号：05", card.seatText)
        assertEquals("备注：正常", card.remarkText)
    }

    @Test
    fun `buildScreenUi备注为空时remarkText为null`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(listOf(exam()), loading = false, error = "", nowMs = 0L)

        assertNull(ui.cards[0].remarkText)
    }

    @Test
    fun `buildScreenUi空数据提示暂无考试安排`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(emptyList(), loading = false, error = "", nowMs = 0L)

        assertEquals("暂无考试安排", ui.statusText)
    }

    @Test
    fun `buildScreenUi错误优先于空状态`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(emptyList(), loading = false, error = "加载失败", nowMs = 0L)

        assertEquals("加载失败", ui.statusText)
    }

    @Test
    fun `buildScreenUi时间缺省待定`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(listOf(exam(time = "")), loading = false, error = "", nowMs = 0L)

        assertEquals("考试日期:待定", ui.cards[0].dateText)
        assertEquals("考试时间:待定", ui.cards[0].timeText)
    }
}
