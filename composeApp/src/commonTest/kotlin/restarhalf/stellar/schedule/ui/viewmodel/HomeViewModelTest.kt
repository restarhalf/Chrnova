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
import restarhalf.stellar.schedule.domain.usecase.BuildHomeClockSnapshotUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeHeaderUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeGreetingUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRenderRowsUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRowUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodSectionsUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.ResolveCourseStatusUseCase
import restarhalf.stellar.schedule.ui.components.screen.home.ExamUi
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
 * HomeViewModel 单元测试。
 *
 * 全部真实 UseCase 链（时钟快照/今日课程/分组/行渲染/表面 UI/问候语），
 * 只 mock 4 个端口（courseRepository/examinationRepository/auth/timetable）。
 * uiState 是 combine 4 流 + stateIn(WhileSubscribed)：subscribeUi + awaitState 模板。
 */
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

    /** 2026-03-02（周一）00:00 本地时区 */
    private val termStartMs = LocalDateTime.parse("2026-03-02T00:00")
        .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    /** 2026-03-04 10:30（周三）本地时区 */
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
        // 全部 stub 先于构造
        every { courseRepository.observeAllCourses() } returns coursesFlow
        every { auth.observeProfile() } returns MutableStateFlow(JwxtAuthProfile(userNo = userNo))
        every { examinationRepository.observeAllExaminations() } returns examsAllFlow
        every { examinationRepository.observeExaminationsByUserNo(any()) } returns examsForUserFlow
        every { timetable.getCampusTimetable(any()) } returns timetableSlots
        val todaySchedule = BuildHomeTodayScheduleUseCase()
        return HomeViewModel(
            courseRepository = courseRepository,
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

    /** uiState 是 stateIn(WhileSubscribed)，须先订阅才有 combine 流更新 */
    private fun TestScope.subscribeUi(vm: HomeViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    /**
     * 轮询等待 uiState 满足谓词。
     *
     * 注意：不能调用 advanceMain()——HomeViewModel._nowMs 是 while(true)+delay(60s) 无限流，
     * advanceUntilIdle 会死循环（同 CourseSelection stopSnatch 教训）。
     * subscribeUi 用 UnconfinedTestDispatcher，combine 链发射即时传播，真实线程轮询即可。
     */
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

    // region uiState

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
        // userNo 为空 → 不做过滤，全量保留
        assertEquals(2, state.exams.size)
    }

    @Test
    fun `uiState按学号过滤考试`() = runTest {
        val vm = makeViewModel(userNo = "20230001")
        subscribeUi(vm)
        awaitState(vm) { true }

        val mine = exam(userNo = "20230001")
        // 模拟 repo 层未过滤干净，VM combine 再按 userNo 过滤
        val impurity = exam(courseName = "大学物理", userNo = "999")
        examsForUserFlow.value = listOf(mine, impurity)
        val state = awaitState(vm) { it.exams.isNotEmpty() }
        assertEquals(listOf(mine), state.exams)
    }

    // endregion

    // region getCampusTimetable

    @Test
    fun `getCampusTimetable透传端口`() = runTest {
        val vm = makeViewModel()
        assertEquals(timetableSlots, vm.getCampusTimetable(Campus.Jinshitan))
        verify { timetable.getCampusTimetable(Campus.Jinshitan) }
    }

    // endregion

    // region buildHomeRenderState（真实 UseCase 端到端）

    @Test
    fun `buildHomeRenderState完整渲染`() = runTest {
        val vm = makeViewModel()
        // 周三：2-3 节课 + 晚上第 9 节课；周一课不显示
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
        // 头部：2026-03-04 周三
        assertEquals("3月4日 星期三", render.headerUi.dateLabel)
        assertTrue(render.headerUi.greeting.isNotBlank())
        // 今日课程：activeWeek=1（diffDays=2 → 第 1 周），周三课 2 门
        assertEquals(1, render.todaySchedule.activeWeek)
        assertEquals(listOf(cWed, cWedLate), render.todaySchedule.todayCourses)
        // 无 1-1 节课 → 无早八
        assertFalse(render.todaySchedule.hasFirstClass)
        // 三个时间段
        assertEquals(listOf("上午课程", "下午课程", "晚上课程"), render.sectionRenders.map { it.title })
        // 上午：1-1 空闲 + 2-3 有课 + 4-4 空闲
        val morning = render.sectionRenders[0].rows
        assertEquals(3, morning.size)
        assertEquals("空闲", morning[0].rowUi.primaryText)
        assertTrue(morning[0].rowUi.isPastOrEmpty)
        assertNull(morning[0].status)
        assertEquals("数据结构", morning[1].rowUi.primaryText)
        assertEquals("8:55", morning[1].timeRange.first)
        assertEquals("10:45", morning[1].timeRange.second)
        // nowMinutes=630（10:30）落在 8:55-10:45 之间 → 进行中
        assertEquals("进行中", morning[1].status)
        assertFalse(morning[1].rowUi.isPastOrEmpty)
        assertEquals("数据结构", morning[1].accentCourseName)
        // 下午：无课 → 单行空闲
        val afternoon = render.sectionRenders[1].rows
        assertEquals(1, afternoon.size)
        assertEquals("空闲", afternoon[0].rowUi.primaryText)
        assertEquals("第5-8节", afternoon[0].rowUi.secondaryText)
        // 晚上：9-10 有课（未开始）+ 11-12 空闲
        val evening = render.sectionRenders[2].rows
        assertEquals(2, evening.size)
        assertEquals("体育", evening[0].rowUi.primaryText)
        assertEquals("18:00", evening[0].timeRange.first)
        assertEquals("19:40", evening[0].timeRange.second)
        assertEquals("未开始", evening[0].status)
        // 表面 UI：有背景 → IMAGE_OVERLAY + alpha 透传
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
        // termStart 前 1 天 → 假期
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
        // 无课程 → 每时段单行空闲
        assertTrue(render.sectionRenders.all { it.rows.size == 1 && it.rows[0].rowUi.primaryText == "空闲" })
        // 无背景 → PRIMARY_SOLID + alpha 收敛 1f
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
        // 上午段：1-1 有课 + 2-4 空闲
        val morning = render.sectionRenders[0].rows
        assertEquals(2, morning.size)
        assertEquals("早八数学", morning[0].rowUi.primaryText)
        assertEquals("8:00", morning[0].timeRange.first)
        assertEquals("8:45", morning[0].timeRange.second)
        // 10:30 > 8:45 → 已结束
        assertEquals("已结束", morning[0].status)
        assertTrue(morning[0].rowUi.isPastOrEmpty)
    }

    // endregion

    // region getTodayExams

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
        // 过滤掉明天 + 按开始时间升序
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
        // 无空格 → substringAfter 返回原串"2026-09-06" > "08:00-10:00"
        assertEquals(listOf(withTime, noTime), today)
    }

    // endregion

    // region buildExamUiList

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
        // isExamStarted 解析失败 → false；IsExamNotEnded 解析失败 → true → isEnded=false
        assertFalse(ui.isStarted)
        assertFalse(ui.isEnded)
    }

    // endregion
}
