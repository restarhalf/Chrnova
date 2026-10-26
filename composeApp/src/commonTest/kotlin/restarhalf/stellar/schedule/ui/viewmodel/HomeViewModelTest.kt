package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
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
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.ui.model.home.BuildHomeClockSnapshotUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeHeaderUiUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeGreetingUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodRenderRowsUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodRowUiUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodSectionsUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.ui.model.home.BuildHomeTodayScheduleUseCase
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllCoursesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.ui.model.home.ResolveCourseStatusUseCase
import restarhalf.stellar.schedule.ui.model.ExamUi
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

class HomeViewModelTest {

    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)

    private val coursesFlow = MutableStateFlow<List<Course>>(emptyList())
    private val examsAllFlow = MutableStateFlow<List<Examination>>(emptyList())
    private val examsForUserFlow = MutableStateFlow<List<Examination>>(emptyList())
    private val profileFlow = MutableStateFlow(JwxtAuthProfile(userNo = ""))

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

    private val termStartMs = LocalDateTime.parse("2026-03-02T00:00")
        .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    private val nowMs = LocalDateTime.parse("2026-03-04T10:30:00")
        .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

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
        weeks: List<Int> = listOf(1),
    ) = Course(
        name = name,
        location = location,
        teacher = teacher,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#FF0000",
    )

    private fun exam(
        courseName: String = "高等数学",
        courseNumber: String = "CS001",
        time: String = "2026-09-06 14:00-16:00",
        place: String = "一教101",
        userNo: String = "20230001",
    ) = Examination(
        courseName = courseName,
        courseNumber = courseNumber,
        time = time,
        examinationPlace = place,
        userNo = userNo,
    )

    private fun makeViewModel(userNo: String = ""): HomeViewModel {

        every { courseRepository.observeAllCourses() } returns coursesFlow
        every { auth.observeProfile() } returns MutableStateFlow(JwxtAuthProfile(userNo = userNo))
        every { examinationRepository.observeAllExaminations() } returns examsAllFlow
        every { examinationRepository.observeExaminationsByUserNo(any()) } returns examsForUserFlow
        every { timetable.getCampusTimetable(any()) } returns timetableSlots
        val todaySchedule = BuildHomeTodayScheduleUseCase()
        return HomeViewModel(
            observeAllCourses = ObserveAllCoursesUseCase(courseRepository),
            observeAllExaminations = ObserveAllExaminationsUseCase(examinationRepository, auth),
            auth = auth,
            isExamNotEnded = IsExamNotEndedUseCase(),
            timetable = timetable,
            buildHomeClockSnapshotUseCase = BuildHomeClockSnapshotUseCase(),
            buildHomeTodayScheduleUseCase = todaySchedule,
            buildHomeHeaderUiUseCase = BuildHomeHeaderUiUseCase(BuildHomeGreetingUseCase()),
            buildHomePeriodSectionsUseCase = BuildHomePeriodSectionsUseCase(),
            buildHomePeriodRenderRowsUseCase = BuildHomePeriodRenderRowsUseCase(
                todaySchedule, ResolveCourseStatusUseCase(), BuildHomePeriodRowUiUseCase(),
            ),
            buildHomeSurfaceUiUseCase = BuildHomeSurfaceUiUseCase(),
        )
    }

    private fun TestScope.subscribeUi(vm: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    private suspend fun awaitState(
        vm: HomeViewModel,
        timeoutMs: Long = 5000,
        predicate: (HomeViewModel.HomeUiState) -> Boolean,
    ): HomeViewModel.HomeUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        while (Clock.System.now().toEpochMilliseconds() < deadline) {
            val state = vm.uiState.value
            if (predicate(state)) return state
            withContext(Dispatchers.Default) { delay(10) }
        }
        error("等待 uiState 超时，当前: ${vm.uiState.value}")
    }

    @Test
    fun `uiState初值为空`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        val state = awaitState(vm) { true }
        assertTrue(state.courses.isEmpty())
        assertTrue(state.exams.isEmpty())
        assertTrue(state.nowMs > 0)
    }

    @Test
    fun `uiState响应课程流`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)
        awaitState(vm) { true }

        val c1 = course()
        coursesFlow.value = listOf(c1)
        val state = awaitState(vm) { it.courses.isNotEmpty() }
        assertEquals(listOf(c1), state.courses)
    }

    @Test
    fun `uiState学号为空时不过滤考试`() = runTest {
        val vm = makeViewModel(userNo = "")
        subscribeUi(vm)
        awaitState(vm) { true }

        val mine = exam(userNo = "20230001")
        val others = exam(courseName = "大学物理", userNo = "999")
        examsAllFlow.value = listOf(mine, others)
        val state = awaitState(vm) { it.exams.isNotEmpty() }

        assertEquals(2, state.exams.size)
    }

    @Test
    fun `uiState按学号过滤考试`() = runTest {
        val vm = makeViewModel(userNo = "20230001")
        subscribeUi(vm)
        awaitState(vm) { true }

        val mine = exam(userNo = "20230001")

        val impurity = exam(courseName = "大学物理", userNo = "999")
        examsForUserFlow.value = listOf(mine, impurity)
        val state = awaitState(vm) { it.exams.isNotEmpty() }
        assertEquals(listOf(mine), state.exams)
    }

    @Test
    fun `getCampusTimetable透传端口`() = runTest {
        val vm = makeViewModel()
        assertEquals(timetableSlots, vm.getCampusTimetable(Campus.Jinshitan))
        verify { timetable.getCampusTimetable(Campus.Jinshitan) }
    }

    @Test
    fun `buildHomeRenderState完整渲染`() = runTest {
        val vm = makeViewModel()

        val cWed = course(name = "数据结构", dayOfWeek = 3, startSection = 2, sectionCount = 2)
        val cWedLate = course(name = "体育", dayOfWeek = 3, startSection = 9, sectionCount = 2)
        val cMon = course(name = "高等数学", dayOfWeek = 1, startSection = 1, sectionCount = 2)

        val render = vm.buildHomeRenderState(
            courses = listOf(cMon, cWed, cWedLate),
            campus = Campus.Jinshitan,
            termStartMs = termStartMs,
            totalWeeks = 20,
            hasBackground = true,
            componentsAlpha = 0.8f,
            nowMs = nowMs,
        )

        assertEquals("3月4日 星期三", render.headerUi.dateLabel)
        assertTrue(render.headerUi.greeting.isNotBlank())

        assertEquals(1, render.todaySchedule.activeWeek)
        assertEquals(listOf(cWed, cWedLate), render.todaySchedule.todayCourses)

        assertFalse(render.todaySchedule.hasFirstClass)

        assertEquals(listOf("上午课程", "下午课程", "晚上课程"), render.sectionRenders.map { it.title })

        val morning = render.sectionRenders[0].rows
        assertEquals(3, morning.size)
        assertEquals("空闲", morning[0].rowUi.primaryText)
        assertTrue(morning[0].rowUi.isPastOrEmpty)
        assertNull(morning[0].status)
        assertEquals("数据结构", morning[1].rowUi.primaryText)
        assertEquals("8:55", morning[1].timeRange.first)
        assertEquals("10:45", morning[1].timeRange.second)

        assertEquals("进行中", morning[1].status)
        assertFalse(morning[1].rowUi.isPastOrEmpty)
        assertEquals("数据结构", morning[1].accentCourseName)

        val afternoon = render.sectionRenders[1].rows
        assertEquals(1, afternoon.size)
        assertEquals("空闲", afternoon[0].rowUi.primaryText)
        assertEquals("第5-8节", afternoon[0].rowUi.secondaryText)

        val evening = render.sectionRenders[2].rows
        assertEquals(2, evening.size)
        assertEquals("体育", evening[0].rowUi.primaryText)
        assertEquals("18:00", evening[0].timeRange.first)
        assertEquals("19:40", evening[0].timeRange.second)
        assertEquals("未开始", evening[0].status)

        assertEquals(
            BuildHomeSurfaceUiUseCase.HeaderBackgroundMode.IMAGE_OVERLAY,
            render.surfaceUi.headerBackgroundMode,
        )
        assertEquals(0.8f, render.surfaceUi.contentSurfaceAlpha)
        assertEquals(630, render.nowMinutes)
    }

    @Test
    fun `buildHomeRenderState假期周今日课程为空`() = runTest {
        val vm = makeViewModel()

        val render = vm.buildHomeRenderState(
            courses = listOf(course(name = "高等数学", dayOfWeek = 3, weeks = listOf(1))),
            campus = Campus.Jinshitan,
            termStartMs = termStartMs,
            totalWeeks = 20,
            hasBackground = false,
            componentsAlpha = 2f,
            nowMs = termStartMs - 24 * 60 * 60 * 1000L,
        )
        assertEquals(null, render.todaySchedule.activeWeek)
        assertTrue(render.todaySchedule.todayCourses.isEmpty())

        assertTrue(render.sectionRenders.all { it.rows.size == 1 && it.rows[0].rowUi.primaryText == "空闲" })

        assertEquals(
            BuildHomeSurfaceUiUseCase.HeaderBackgroundMode.PRIMARY_SOLID,
            render.surfaceUi.headerBackgroundMode,
        )
        assertEquals(1f, render.surfaceUi.contentSurfaceAlpha)
    }

    @Test
    fun `buildHomeRenderState有早八课hasFirstClass`() = runTest {
        val vm = makeViewModel()
        val early = course(name = "早八数学", dayOfWeek = 3, startSection = 1, sectionCount = 1)
        val render = vm.buildHomeRenderState(
            courses = listOf(early),
            campus = Campus.Jinshitan,
            termStartMs = termStartMs,
            totalWeeks = 20,
            hasBackground = false,
            componentsAlpha = 0.5f,
            nowMs = nowMs,
        )
        assertTrue(render.todaySchedule.hasFirstClass)

        val morning = render.sectionRenders[0].rows
        assertEquals(2, morning.size)
        assertEquals("早八数学", morning[0].rowUi.primaryText)
        assertEquals("8:00", morning[0].timeRange.first)
        assertEquals("8:45", morning[0].timeRange.second)

        assertEquals("已结束", morning[0].status)
        assertTrue(morning[0].rowUi.isPastOrEmpty)
    }

    @Test
    fun `getTodayExams按日期过滤并按开始时间排序`() = runTest {
        val vm = makeViewModel()
        val afternoon = exam(time = "2026-09-06 14:00-16:00")
        val morning = exam(courseName = "大学物理", time = "2026-09-06 09:00-11:00")
        val tomorrow = exam(courseName = "有机化学", time = "2026-09-07 09:00-11:00")

        val today = vm.getTodayExams(
            exams = listOf(afternoon, morning, tomorrow),
            nowMs = LocalDateTime.parse("2026-09-06T12:00:00")
                .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        )

        assertEquals(listOf(morning, afternoon), today)
    }

    @Test
    fun `getTodayExams无时间部分回退午夜排序`() = runTest {
        val vm = makeViewModel()
        val noTime = exam(courseName = "体测", time = "2026-09-06")
        val withTime = exam(courseName = "高等数学", time = "2026-09-06 08:00-10:00")
        val today = vm.getTodayExams(
            exams = listOf(noTime, withTime),
            nowMs = LocalDateTime.parse("2026-09-06T12:00:00")
                .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
        )

        assertEquals(listOf(withTime, noTime), today)
    }

    @Test
    fun `buildExamUiList横杠时间解析`() = runTest {
        val vm = makeViewModel()
        val startedMs = LocalDateTime.parse("2026-09-06T15:00:00")
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val uiList = vm.buildExamUiList(
            exams = listOf(exam(time = "2026-09-06 14:00-16:00")),
            nowMs = startedMs,
        )
        assertEquals(1, uiList.size)
        val ui = uiList[0]
        assertEquals("高等数学", ui.title)
        assertEquals("14:00", ui.startTime)
        assertEquals("16:00", ui.endTime)
        assertEquals("一教101", ui.location)
        assertEquals("高等数学", ui.accentCourseName)
        assertTrue(ui.isStarted)
        assertFalse(ui.isEnded)
    }

    @Test
    fun `buildExamUiList波浪号时间解析`() = runTest {
        val vm = makeViewModel()
        val startedMs = LocalDateTime.parse("2026-09-06T15:00:00")
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val ui = vm.buildExamUiList(
            exams = listOf(exam(time = "2026-09-06 14:00~16:00")),
            nowMs = startedMs,
        )[0]
        assertEquals("14:00", ui.startTime)
        assertEquals("16:00", ui.endTime)
    }

    @Test
    fun `buildExamUiList未开始与已结束状态`() = runTest {
        val vm = makeViewModel()
        val beforeMs = LocalDateTime.parse("2026-09-06T10:00:00")
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val afterMs = LocalDateTime.parse("2026-09-06T17:00:00")
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

        val before = vm.buildExamUiList(listOf(exam()), nowMs = beforeMs)[0]
        assertFalse(before.isStarted)
        assertFalse(before.isEnded)

        val after = vm.buildExamUiList(listOf(exam()), nowMs = afterMs)[0]
        assertTrue(after.isStarted)
        assertTrue(after.isEnded)
    }

    @Test
    fun `buildExamUiList字段回退`() = runTest {
        val vm = makeViewModel()
        val ui = vm.buildExamUiList(
            exams = listOf(
                exam(courseName = "", courseNumber = "CS101", place = ""),
            ),
            nowMs = nowMs,
        )[0]
        assertEquals("未命名课程", ui.title)
        assertEquals("待定", ui.location)
        assertEquals("CS101", ui.accentCourseName)
    }

    @Test
    fun `buildExamUiList无法解析时间不崩溃`() = runTest {
        val vm = makeViewModel()
        val ui = vm.buildExamUiList(
            exams = listOf(exam(time = " TBD ")),
            nowMs = nowMs,
        )[0]

        assertFalse(ui.isStarted)
        assertFalse(ui.isEnded)
    }

}
