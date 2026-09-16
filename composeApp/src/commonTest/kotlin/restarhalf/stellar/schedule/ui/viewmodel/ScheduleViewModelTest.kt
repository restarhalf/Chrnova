package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.core.time.AcademicCalendar
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.usecase.BuildScheduleUiStateUseCase
import restarhalf.stellar.schedule.domain.usecase.RefreshCourseRemindersIfEnabledUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseUseCase
import restarhalf.stellar.schedule.domain.usecase.TransCourseWithConflictsUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * ScheduleViewModel 单元测试。
 *
 * uiState 是 combine 5 流 + stateIn(WhileSubscribed)：subscribeUi + awaitState 模板。
 * 三个 UseCase 全部用真实实例（BuildScheduleUiState/TransCourseWithConflicts/RefreshCourseRemindersIfEnabled），
 * 只 mock 端口（settings/courseRepository/timetable/courseReminder）。
 * shouldAutoSync/insertCourse/deleteCourse/refreshCourseRemindersIfEnabled 走 withContext(AppIoDispatcher) 真实跨线程。
 */
class ScheduleViewModelTest {

    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val courseReminder = mock<CourseReminderPort>(MockMode.autofill)

    private val showNonCurrentWeekFlow = MutableStateFlow(true)
    private val rowHeightFlow = MutableStateFlow(SettingsPort.DEFAULT_ROW_HEIGHT_DP)
    private val reminderFlow = MutableStateFlow(false)
    private val coursesFlow = MutableStateFlow<List<Course>>(emptyList())

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

    /** 2026-03-02（周一）00:00 本地时区 */
    private val termStartMs = LocalDateTime.parse("2026-03-02T00:00")
        .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    private val dayMs = 24 * 60 * 60 * 1000L

    /** 10 节时间槽（与真实金石滩配置前 10 节一致） */
    private val timetableSlots = listOf(
        TimetableSlot(1, "8:00", "8:45"),
        TimetableSlot(2, "8:55", "9:40"),
        TimetableSlot(3, "10:00", "10:45"),
        TimetableSlot(4, "10:55", "11:40"),
        TimetableSlot(5, "13:30", "14:15"),
        TimetableSlot(6, "14:25", "15:10"),
        TimetableSlot(7, "15:20", "16:05"),
        TimetableSlot(8, "16:15", "17:00"),
        TimetableSlot(9, "18:00", "18:45"),
        TimetableSlot(10, "18:55", "19:40"),
    )

    private fun course(
        name: String = "高等数学",
        location: String = "一教101",
        teacher: String = "张老师",
        dayOfWeek: Int = 1,
        startSection: Int = 1,
        sectionCount: Int = 2,
        weeks: List<Int> = listOf(1, 2),
        type: Int = 0,
        targetWeek: Int = 0,
        remoteKey: String = "rk-1",
    ) = Course(
        name = name,
        location = location,
        teacher = teacher,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#FF0000",
        type = type,
        remoteKey = remoteKey,
        targetWeek = targetWeek,
    )

    private fun makeViewModel(): ScheduleViewModel {
        // 全部 stub 先于构造
        every { settings.observeShowNonCurrentWeek() } returns showNonCurrentWeekFlow
        every { settings.observeScheduleRowHeight() } returns rowHeightFlow
        every { settings.observeCourseReminderEnabled() } returns reminderFlow
        every { courseRepository.observeAllCourses() } returns coursesFlow
        every { timetable.getCampusTimetable(any()) } returns timetableSlots
        return ScheduleViewModel(
            settings = settings,
            courseRepository = courseRepository,
            buildScheduleUiStateUseCase = BuildScheduleUiStateUseCase(timetable),
            transCourseWithConflicts = TransCourseWithConflictsUseCase(
                courseRepository, TransCourseUseCase(),
            ),
            refreshCourseRemindersIfEnabledUseCase = RefreshCourseRemindersIfEnabledUseCase(
                settings = settings,
                courseRepository = courseRepository,
                courseReminder = courseReminder,
            ),
        )
    }

    /** uiState 是 stateIn(WhileSubscribed)，须先订阅才有 combine 流更新 */
    private fun TestScope.subscribeUi(vm: ScheduleViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    /** 轮询等待 uiState 满足谓词，每轮主动 drain Main 队列 */
    private suspend fun awaitState(
        vm: ScheduleViewModel,
        timeoutMs: Long = 5000,
        predicate: (ScheduleViewModel.ScheduleUiState) -> Boolean,
    ): ScheduleViewModel.ScheduleUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        while (Clock.System.now().toEpochMilliseconds() < deadline) {
            advanceMain()
            val state = vm.uiState.value
            if (predicate(state)) return state
            withContext(Dispatchers.Default) { delay(10) }
        }
        error("等待 uiState 超时，当前: ${vm.uiState.value}")
    }

    // region uiState / allCourses / detailSheet

    @Test
    fun `uiState初值为默认配置`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        val state = awaitState(vm) { true }
        assertTrue(state.showNonCurrentWeek)
        assertEquals(SettingsPort.DEFAULT_ROW_HEIGHT_DP, state.scheduleRowHeight)
        assertFalse(state.transDialogUiState.show)
        assertFalse(state.transConflictUiState.show)
        assertFalse(state.detailSheetUiState.show)
    }

    @Test
    fun `uiState响应settings流更新`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        showNonCurrentWeekFlow.value = false
        rowHeightFlow.value = 80
        val state = awaitState(vm) { !it.showNonCurrentWeek && it.scheduleRowHeight == 80 }
        assertFalse(state.showNonCurrentWeek)
        assertEquals(80, state.scheduleRowHeight)
    }

    @Test
    fun `allCourses响应课程流更新`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        // allCourses 也是 stateIn(WhileSubscribed)，须同样订阅才会收集上游
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.allCourses.collect {}
        }
        awaitState(vm) { true }

        val c1 = course()
        coursesFlow.value = listOf(c1)
        val deadline = Clock.System.now().toEpochMilliseconds() + 5000
        while (vm.allCourses.value.isEmpty() && Clock.System.now().toEpochMilliseconds() < deadline) {
            advanceMain()
            withContext(Dispatchers.Default) { delay(10) }
        }
        assertEquals(listOf(c1), vm.allCourses.value)
    }

    @Test
    fun `openDetailSheet有课时显示`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val c1 = course()
        vm.openDetailSheet(listOf(c1))
        val state = awaitState(vm) { it.detailSheetUiState.show }
        assertEquals(listOf(c1), state.detailSheetUiState.courses)
    }

    @Test
    fun `openDetailSheet空列表不显示`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.openDetailSheet(emptyList())
        advanceMain()
        assertFalse(vm.uiState.value.detailSheetUiState.show)
        assertTrue(vm.uiState.value.detailSheetUiState.courses.isEmpty())
    }

    @Test
    fun `closeDetailSheet复位`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.openDetailSheet(listOf(course()))
        awaitState(vm) { it.detailSheetUiState.show }
        vm.closeDetailSheet()
        val state = awaitState(vm) { !it.detailSheetUiState.show }
        assertTrue(state.detailSheetUiState.courses.isEmpty())
    }

    @Test
    fun `openDetailSheet覆盖旧状态`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val c1 = course(name = "大学物理")
        vm.openDetailSheet(listOf(course(name = "高等数学")))
        awaitState(vm) { it.detailSheetUiState.show }
        vm.openDetailSheet(listOf(c1))
        val state = awaitState(vm) { it.detailSheetUiState.courses == listOf(c1) }
        assertTrue(state.detailSheetUiState.show)
    }

    // endregion

    // region transDialog

    @Test
    fun `openTransDialog填充字段`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val c1 = course(
            location = "二教202", dayOfWeek = 3, startSection = 5, sectionCount = 2,
        )
        vm.openTransDialog(c1, currentWeek = 4)
        val state = awaitState(vm) { it.transDialogUiState.show }
        val dialog = state.transDialogUiState
        assertEquals(c1, dialog.course)
        assertEquals(4, dialog.targetWeek)
        assertEquals(4, dialog.originWeek)
        assertEquals("二教202", dialog.newClassRoom.text)
        assertEquals(3, dialog.dayOfWeek)
        assertEquals(5, dialog.startSection)
        assertEquals(6, dialog.endSection)
    }

    @Test
    fun `openTransDialog周次下限收敛1`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.openTransDialog(course(), currentWeek = 0)
        val dialog = awaitState(vm) { it.transDialogUiState.show }.transDialogUiState
        assertEquals(1, dialog.targetWeek)
        assertEquals(1, dialog.originWeek)
    }

    @Test
    fun `openTransDialog字段边界收敛`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        // dayOfWeek=99 越界、start=11 + count=3 → end=13 越界
        val c1 = course(dayOfWeek = 99, startSection = 11, sectionCount = 3)
        vm.openTransDialog(c1, currentWeek = 2)
        val dialog = awaitState(vm) { it.transDialogUiState.show }.transDialogUiState
        assertEquals(7, dialog.dayOfWeek)
        assertEquals(11, dialog.startSection)
        assertEquals(12, dialog.endSection)
    }

    @Test
    fun `dismissTransDialog仅隐藏保留数据`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val c1 = course()
        vm.openTransDialog(c1, currentWeek = 2)
        awaitState(vm) { it.transDialogUiState.show }
        vm.dismissTransDialog()
        val state = awaitState(vm) { !it.transDialogUiState.show }
        // 数据保留：course 与周次不清除
        assertEquals(c1, state.transDialogUiState.course)
        assertEquals(2, state.transDialogUiState.originWeek)
    }

    @Test
    fun `closeTransDialogAndClear全复位`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.openTransDialog(course(), currentWeek = 2)
        awaitState(vm) { it.transDialogUiState.show }
        vm.closeTransDialogAndClear()
        val state = awaitState(vm) { !it.transDialogUiState.show }
        assertNull(state.transDialogUiState.course)
        assertEquals(1, state.transDialogUiState.targetWeek)
        assertEquals(1, state.transDialogUiState.originWeek)
        assertEquals(1, state.transDialogUiState.dayOfWeek)
        assertEquals("", state.transDialogUiState.newClassRoom.text)
    }

    @Test
    fun `updateTransTargetWeek更新目标周`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.openTransDialog(course(), currentWeek = 2)
        awaitState(vm) { it.transDialogUiState.show }
        vm.updateTransTargetWeek(7)
        val dialog = awaitState(vm) { it.transDialogUiState.targetWeek == 7 }.transDialogUiState
        assertEquals(2, dialog.originWeek)
    }

    @Test
    fun `updateTransNewClassRoom更新教室`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.openTransDialog(course(), currentWeek = 2)
        awaitState(vm) { it.transDialogUiState.show }
        vm.updateTransNewClassRoom(TextFieldValue("三教303"))
        val dialog = awaitState(vm) { it.transDialogUiState.newClassRoom.text == "三教303" }.transDialogUiState
        assertEquals("三教303", dialog.newClassRoom.text)
    }

    @Test
    fun `updateTransDayOfWeek边界收敛`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.openTransDialog(course(), currentWeek = 2)
        awaitState(vm) { it.transDialogUiState.show }
        vm.updateTransDayOfWeek(0)
        assertEquals(1, awaitState(vm) { it.transDialogUiState.dayOfWeek == 1 }.transDialogUiState.dayOfWeek)
        vm.updateTransDayOfWeek(9)
        assertEquals(7, awaitState(vm) { it.transDialogUiState.dayOfWeek == 7 }.transDialogUiState.dayOfWeek)
    }

    @Test
    fun `updateTransSectionRange边界收敛`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.openTransDialog(course(), currentWeek = 2)
        awaitState(vm) { it.transDialogUiState.show }
        vm.updateTransSectionRange(0, 99)
        val dialog = awaitState(vm) { it.transDialogUiState.startSection == 1 }.transDialogUiState
        assertEquals(1, dialog.startSection)
        assertEquals(12, dialog.endSection)
    }

    @Test
    fun `buildTransOperationInput无课程返回null`() = runTest {
        val vm = makeViewModel()
        assertNull(vm.buildTransOperationInput())
    }

    @Test
    fun `buildTransOperationInput字段映射`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val c1 = course(location = "一教101")
        vm.openTransDialog(c1, currentWeek = 3)
        awaitState(vm) { it.transDialogUiState.show }
        vm.updateTransTargetWeek(5)
        vm.updateTransNewClassRoom(TextFieldValue("四教404"))
        vm.updateTransDayOfWeek(2)
        vm.updateTransSectionRange(3, 4)

        val input = vm.buildTransOperationInput()
        assertNotNull(input)
        assertEquals(c1, input.course)
        assertEquals(3, input.originWeek)
        assertEquals(5, input.targetWeek)
        assertEquals("四教404", input.newRoom)
        assertEquals(2, input.dayOfWeek)
        assertEquals(3, input.startSection)
        assertEquals(4, input.endSection)
    }

    // endregion

    // region transConflict

    @Test
    fun `showTransConflict显示冲突与待覆盖课程`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val conflict = course(name = "大学物理", dayOfWeek = 2)
        val pending = course(name = "高等数学", dayOfWeek = 2)
        vm.showTransConflict(listOf(conflict), pending)
        val state = awaitState(vm) { it.transConflictUiState.show }
        assertEquals(listOf(conflict), state.transConflictUiState.conflicts)
        assertEquals(pending, state.transConflictUiState.pendingOverride)
    }

    @Test
    fun `dismissTransConflict不重开对话框`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.showTransConflict(listOf(course()), course())
        awaitState(vm) { it.transConflictUiState.show }
        vm.dismissTransConflict(reopenTransDialog = false)
        val state = awaitState(vm) { !it.transConflictUiState.show }
        assertFalse(state.transDialogUiState.show)
        // 冲突数据保留
        assertEquals(1, state.transConflictUiState.conflicts.size)
    }

    @Test
    fun `dismissTransConflict有课程时重开对话框`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val c1 = course()
        vm.openTransDialog(c1, currentWeek = 2)
        awaitState(vm) { it.transDialogUiState.show }
        vm.dismissTransDialog() // 先隐藏对话框（数据保留）
        awaitState(vm) { !it.transDialogUiState.show }
        vm.showTransConflict(listOf(course(name = "大学物理")), c1)
        awaitState(vm) { it.transConflictUiState.show }
        vm.dismissTransConflict(reopenTransDialog = true)
        val state = awaitState(vm) { it.transDialogUiState.show }
        // 冲突隐藏 + 对话框重开且数据保留
        assertFalse(state.transConflictUiState.show)
        assertEquals(c1, state.transDialogUiState.course)
    }

    @Test
    fun `dismissTransConflict无课程时不重开`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.showTransConflict(listOf(course()), course())
        awaitState(vm) { it.transConflictUiState.show }
        vm.dismissTransConflict(reopenTransDialog = true)
        val state = awaitState(vm) { !it.transConflictUiState.show }
        assertFalse(state.transDialogUiState.show)
    }

    @Test
    fun `clearTransConflict复位`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        vm.showTransConflict(listOf(course()), course())
        awaitState(vm) { it.transConflictUiState.show }
        vm.clearTransConflict()
        val state = awaitState(vm) { !it.transConflictUiState.show }
        assertTrue(state.transConflictUiState.conflicts.isEmpty())
        assertNull(state.transConflictUiState.pendingOverride)
    }

    @Test
    fun `consumePendingOverride返回并清空`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val pending = course(name = "高等数学")
        vm.showTransConflict(listOf(course(name = "大学物理")), pending)
        awaitState(vm) { it.transConflictUiState.show }

        assertEquals(pending, vm.consumePendingOverride())
        // 消费后整体复位
        val state = awaitState(vm) { !it.transConflictUiState.show }
        assertTrue(state.transConflictUiState.conflicts.isEmpty())
        assertNull(state.transConflictUiState.pendingOverride)
        // 再次消费返回 null
        assertNull(vm.consumePendingOverride())
    }

    // endregion

    // region page/week 转换

    @Test
    fun `pageToWeek与weekToPage双向转换`() = runTest {
        val vm = makeViewModel()
        // 含第 0 周：page 与 week 一致
        assertEquals(3, vm.pageToWeek(page = 3, includeWeek0 = true))
        assertEquals(3, vm.weekToPage(week = 3, includeWeek0 = true))
        // 不含第 0 周：偏移 1
        assertEquals(3, vm.pageToWeek(page = 2, includeWeek0 = false))
        assertEquals(2, vm.weekToPage(week = 3, includeWeek0 = false))
    }

    // endregion

    // region buildScheduleUiState（真实 UseCase）

    @Test
    fun `buildScheduleUiState学期中第3周`() = runTest {
        val vm = makeViewModel()
        // termStart + 14 天 = 2026-03-16（第 3 周周一）
        val state = vm.buildScheduleUiState(
            campus = Campus.Jinshitan,
            totalWeeks = 20,
            termStartMs = termStartMs,
            nowMs = termStartMs + 14 * dayMs,
        )
        assertEquals(3, state.detectedWeekInfo.week)
        assertEquals(14, state.detectedWeekInfo.diffDays)
        assertFalse(state.detectedWeekInfo.isHoliday)
        assertFalse(state.includeWeek0)
        assertEquals(2, state.pagerInitialPage)
        assertEquals(20, state.pagerPageCount)
        assertEquals(timetableSlots, state.timetable)
    }

    @Test
    fun `buildScheduleUiState学期开始前为假期`() = runTest {
        val vm = makeViewModel()
        val state = vm.buildScheduleUiState(
            campus = Campus.Jinshitan,
            totalWeeks = 20,
            termStartMs = termStartMs,
            nowMs = termStartMs - dayMs,
        )
        assertEquals(0, state.detectedWeekInfo.week)
        assertEquals(-1, state.detectedWeekInfo.diffDays)
        assertTrue(state.detectedWeekInfo.isHoliday)
        assertTrue(state.includeWeek0)
        assertEquals(0, state.pagerInitialPage)
        assertEquals(21, state.pagerPageCount)
    }

    @Test
    fun `buildScheduleUiState超过总周数为假期`() = runTest {
        val vm = makeViewModel()
        // totalWeeks=2，termStart + 21 天 → week=4 > 2
        val state = vm.buildScheduleUiState(
            campus = Campus.Jinshitan,
            totalWeeks = 2,
            termStartMs = termStartMs,
            nowMs = termStartMs + 21 * dayMs,
        )
        assertTrue(state.detectedWeekInfo.isHoliday)
        assertEquals(0, state.detectedWeekInfo.week)
        assertTrue(state.includeWeek0)
        assertEquals(3, state.pagerPageCount)
    }

    // endregion

    // region buildWeekHeaderUi

    @Test
    fun `buildWeekHeaderUi正常周`() = runTest {
        val vm = makeViewModel()
        val header = vm.buildWeekHeaderUi(
            currentWeek = 3,
            detectedDiffDays = 14,
            detectedWeek = 3,
            termStartMs = termStartMs,
            dayCount = 7,
        )
        assertEquals(listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日"), header.days)
        // 第 3 周从 2026-03-16（周一）开始
        assertEquals(AcademicCalendar.getWeekDates(3, termStartMs).take(7), header.dates)
        // day 用 Padding.ZERO 补零，month 不补 → "03/16"
        assertEquals("03/16", header.dates.first())
        // 今日（2026-09 之后）不在第 3 周 → null（动态计算避免运行日期耦合）
        assertEquals(
            AcademicCalendar.getTodayIndexInWeek(3, termStartMs),
            header.todayIndex,
        )
    }

    @Test
    fun `buildWeekHeaderUi第0周未开学回退当前日期`() = runTest {
        val vm = makeViewModel()
        val header = vm.buildWeekHeaderUi(
            currentWeek = 0,
            detectedDiffDays = -5,
            detectedWeek = 0,
            termStartMs = termStartMs,
            dayCount = 7,
        )
        // headerWeek=-1 → 使用当前真实周的日期与今日索引
        assertEquals(AcademicCalendar.getCurrentWeekDates(dayCount = 7), header.dates)
        assertEquals(AcademicCalendar.getTodayIndexInCurrentWeek(), header.todayIndex)
    }

    @Test
    fun `buildWeekHeaderUi第0周检测到周次时使用检测周`() = runTest {
        val vm = makeViewModel()
        val header = vm.buildWeekHeaderUi(
            currentWeek = 0,
            detectedDiffDays = 3,
            detectedWeek = 2,
            termStartMs = termStartMs,
            dayCount = 5,
        )
        // headerWeek=2 → 第 2 周日期，取前 5 天
        assertEquals(AcademicCalendar.getWeekDates(2, termStartMs).take(5), header.dates)
        assertEquals(5, header.dates.size)
    }

    // endregion

    // region buildPageRenderUi

    @Test
    fun `buildPageRenderUi按天分组并区分当前与非当前周`() = runTest {
        val vm = makeViewModel()
        val current = course(name = "高等数学", dayOfWeek = 1, weeks = listOf(2))
        val otherDay = course(name = "大学物理", dayOfWeek = 2, weeks = listOf(2))
        // 非当前周课程需与当前周课程不同节次，否则会被重叠过滤隐藏
        val nonCurrent = course(name = "有机化学", dayOfWeek = 1, startSection = 5, weeks = listOf(5))
        val muted = Color.Red

        val render = vm.buildPageRenderUi(
            courses = listOf(current, otherDay, nonCurrent),
            page = 1, // includeWeek0=false → actualWeek=2
            includeWeek0 = false,
            dayCount = 5,
            showNonCurrentWeek = true,
            isDarkMode = false,
            mutedCourseColor = muted,
            mutedTitleColor = Color.Black,
            mutedSubColor = Color.Gray,
            yForSection = { (it * 10).dp },
            heightForSections = { (it * 64).dp },
            cellInset = 2.dp,
        )
        assertEquals(2, render.actualWeek)
        assertEquals((1..5).toSet(), render.dayRenderData.keys)

        val day1 = render.dayRenderData.getValue(1).items
        // 第 1 天：当前周课程 + 无重叠的非当前周课程
        assertEquals(2, day1.size)
        val currentModel = day1.first { it.model.name == "高等数学" }.model
        assertEquals(12.dp, currentModel.topOffsetY) // yForSection(1)=10 + inset 2
        assertEquals(124.dp, currentModel.height) // heightForSections(2)=128 - inset*2 4
        // 非当前周课程使用 muted 颜色
        val mutedModel = day1.first { it.model.name == "有机化学" }.model
        assertEquals(muted, mutedModel.color)
        // 第 2 天只有当前周课程
        assertEquals(listOf("大学物理"), render.dayRenderData.getValue(2).items.map { it.model.name })
        // 第 5 天无课程
        assertTrue(render.dayRenderData.getValue(5).items.isEmpty())
    }

    @Test
    fun `buildPageRenderUi传入effectiveCourses时优先使用`() = runTest {
        val vm = makeViewModel()
        val all = listOf(course(name = "高等数学", dayOfWeek = 1, weeks = listOf(2)))
        // 传入空 effectiveCourses → 即使 all 有课程也不渲染
        val render = vm.buildPageRenderUi(
            courses = all,
            page = 1,
            includeWeek0 = false,
            dayCount = 3,
            showNonCurrentWeek = true,
            isDarkMode = false,
            mutedCourseColor = Color.Red,
            mutedTitleColor = Color.Black,
            mutedSubColor = Color.Gray,
            yForSection = { (it * 10).dp },
            heightForSections = { (it * 64).dp },
            cellInset = 2.dp,
            effectiveCourses = emptyList(),
        )
        assertTrue(render.dayRenderData.values.all { it.items.isEmpty() })
    }

    // endregion

    // region buildCourseDetailUi

    @Test
    fun `buildCourseDetailUi普通课无标签`() = runTest {
        val vm = makeViewModel()
        val detail = vm.buildCourseDetailUi(
            course = course(weeks = listOf(1, 2), startSection = 1, sectionCount = 2),
            currentWeek = 2,
            timetable = timetableSlots,
        )
        assertEquals("1-2周   |   第1-2 节(8:00-9:40)", detail.weekLine)
        assertEquals("一教101   |   张老师", detail.locationLine)
        assertNull(detail.tagText)
        assertNull(detail.tagStyle)
    }

    @Test
    fun `buildCourseDetailUi实验课标签`() = runTest {
        val vm = makeViewModel()
        val detail = vm.buildCourseDetailUi(
            course = course(weeks = listOf(2), type = 1),
            currentWeek = 2,
            timetable = timetableSlots,
        )
        assertEquals("实验课", detail.tagText)
        assertEquals(ScheduleViewModel.CourseDetailTagStyle.LAB, detail.tagStyle)
    }

    @Test
    fun `buildCourseDetailUi调课标签与周次改写`() = runTest {
        val vm = makeViewModel()
        // type=2, targetWeek=2 == currentWeek，原 weeks=[1] 非空
        val detail = vm.buildCourseDetailUi(
            course = course(weeks = listOf(1), type = 2, targetWeek = 2),
            currentWeek = 2,
            timetable = timetableSlots,
        )
        assertEquals("调课", detail.tagText)
        assertEquals(ScheduleViewModel.CourseDetailTagStyle.TRANS, detail.tagStyle)
        // 周次文本改写为 "第2周（原1周）"
        assertTrue(detail.weekLine.startsWith("第2周（原1周）   |   "))
    }

    @Test
    fun `buildCourseDetailUi调课原周次为空时仅显示目标周`() = runTest {
        val vm = makeViewModel()
        val detail = vm.buildCourseDetailUi(
            course = course(weeks = emptyList(), type = 2, targetWeek = 3),
            currentWeek = 3,
            timetable = timetableSlots,
        )
        assertTrue(detail.weekLine.startsWith("第3周   |   "))
    }

    @Test
    fun `buildCourseDetailUi非本周标签`() = runTest {
        val vm = makeViewModel()
        val detail = vm.buildCourseDetailUi(
            course = course(weeks = listOf(1), type = 0),
            currentWeek = 2,
            timetable = timetableSlots,
        )
        assertEquals("非本周", detail.tagText)
        assertEquals(ScheduleViewModel.CourseDetailTagStyle.NON_CURRENT, detail.tagStyle)
    }

    @Test
    fun `buildCourseDetailUi第0周不显示任何标签`() = runTest {
        val vm = makeViewModel()
        // currentWeek=0：weeks 不含 0 → 实验/调课分支不命中；isCourseActiveInWeek(week<=0)=true → 也不标"非本周"
        val lab = vm.buildCourseDetailUi(
            course = course(weeks = listOf(1), type = 1),
            currentWeek = 0,
            timetable = timetableSlots,
        )
        assertNull(lab.tagText)
        assertNull(lab.tagStyle)
    }

    @Test
    fun `buildCourseDetailUi节次越界时间占位`() = runTest {
        val vm = makeViewModel()
        // start=10, count=2 → end=11 超出 10 节
        val detail = vm.buildCourseDetailUi(
            course = course(weeks = listOf(2), startSection = 10, sectionCount = 2),
            currentWeek = 2,
            timetable = timetableSlots,
        )
        assertEquals("2周   |   第10-11 节(18:55---)", detail.weekLine)
    }

    @Test
    fun `buildCourseDetailUi空时间表双占位`() = runTest {
        val vm = makeViewModel()
        val detail = vm.buildCourseDetailUi(
            course = course(weeks = listOf(2)),
            currentWeek = 2,
            timetable = emptyList(),
        )
        assertEquals("2周   |   第1-2 节(-----)", detail.weekLine)
    }

    // endregion

    // region buildTransCourseAndConflicts（真实 UseCase 链）

    @Test
    fun `buildTransCourseAndConflicts无冲突`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.getAllCoursesOnce() } returns emptyList()

        val origin = course(name = "高等数学", dayOfWeek = 2, remoteKey = "rk-1")
        val result = vm.buildTransCourseAndConflicts(
            originCourse = origin,
            originWeek = 1,
            targetWeek = 3,
            newRoom = "",
            dayOfWeek = 2,
            startSection = 1,
            endSection = 2,
        )
        advanceMain()
        assertTrue(result.conflicts.isEmpty())
        val override = result.overrideCourse
        assertEquals(0, override.id)
        assertEquals(2, override.type)
        assertEquals("rk-1", override.originRemoteKey)
        assertEquals("rk-1#override#1#3", override.remoteKey)
        assertEquals(3, override.targetWeek)
        assertEquals(listOf(1), override.weeks)
        // 新教室空白 → 回退原地点
        assertEquals("一教101", override.location)
    }

    @Test
    fun `buildTransCourseAndConflicts检测到冲突`() = runTest {
        val vm = makeViewModel()
        val existing = course(
            name = "大学物理", dayOfWeek = 2, startSection = 1, sectionCount = 2, weeks = listOf(3),
        )
        everySuspend { courseRepository.getAllCoursesOnce() } returns listOf(existing)

        val origin = course(name = "高等数学", dayOfWeek = 1, remoteKey = "rk-1")
        val result = vm.buildTransCourseAndConflicts(
            originCourse = origin,
            originWeek = 1,
            targetWeek = 3,
            newRoom = "五教505",
            dayOfWeek = 2,
            startSection = 1,
            endSection = 2,
        )
        advanceMain()
        assertEquals(listOf(existing), result.conflicts)
        assertEquals("五教505", result.overrideCourse.location)
    }

    // endregion

    // region insertCourse / deleteCourse / saveTransCourse

    @Test
    fun `insertCourse成功返回true`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.insertCourse(any()) } returns 5L

        val ok = vm.insertCourse(course())
        advanceMain()
        assertTrue(ok)
        verifySuspend { courseRepository.insertCourse(any()) }
    }

    @Test
    fun `insertCourse异常返回false`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.insertCourse(any()) } throws RuntimeException("db down")

        val ok = vm.insertCourse(course())
        advanceMain()
        assertFalse(ok)
    }

    @Test
    fun `deleteCourse成功返回true`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.deleteCourse(any()) } returns Unit

        val ok = vm.deleteCourse(course())
        advanceMain()
        assertTrue(ok)
    }

    @Test
    fun `deleteCourse异常返回false`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.deleteCourse(any()) } throws RuntimeException("db down")

        val ok = vm.deleteCourse(course())
        advanceMain()
        assertFalse(ok)
    }

    @Test
    fun `saveTransCourse委托insertCourse`() = runTest {
        val vm = makeViewModel()
        everySuspend { courseRepository.insertCourse(any()) } returns 9L

        val ok = vm.saveTransCourse(course())
        advanceMain()
        assertTrue(ok)
        verifySuspend(VerifyMode.exactly(1)) { courseRepository.insertCourse(any()) }
    }

    // endregion

    // region shouldAutoSync / refreshCourseRemindersIfEnabled

    @Test
    fun `shouldAutoSync透传端口结果`() = runTest {
        val vm = makeViewModel()
        everySuspend { settings.shouldAutoSyncAndMark(any()) } returns true
        assertTrue(vm.shouldAutoSync())

        everySuspend { settings.shouldAutoSyncAndMark(any()) } returns false
        assertFalse(vm.shouldAutoSync())
        advanceMain()
        verifySuspend(VerifyMode.exactly(2)) { settings.shouldAutoSyncAndMark(any()) }
    }

    @Test
    fun `refreshCourseRemindersIfEnabled提醒关闭时不调度`() = runTest {
        val vm = makeViewModel()
        reminderFlow.value = false

        vm.refreshCourseRemindersIfEnabled(campus = Campus.Jinshitan, termStartMs = termStartMs, totalWeeks = 20)
        advanceMain()
        verify(VerifyMode.not) {
            courseReminder.scheduleNextReminder(any(), any(), any(), any())
        }
    }

    @Test
    fun `refreshCourseRemindersIfEnabled开启时调度下一节`() = runTest {
        val vm = makeViewModel()
        reminderFlow.value = true
        val c1 = course()
        everySuspend { courseRepository.getAllCoursesOnce() } returns listOf(c1)

        vm.refreshCourseRemindersIfEnabled(campus = Campus.Jinshitan, termStartMs = termStartMs, totalWeeks = 20)
        advanceMain()
        verify(VerifyMode.exactly(1)) {
            courseReminder.scheduleNextReminder(listOf(c1), Campus.Jinshitan, termStartMs, 20)
        }
    }

    // endregion
}
