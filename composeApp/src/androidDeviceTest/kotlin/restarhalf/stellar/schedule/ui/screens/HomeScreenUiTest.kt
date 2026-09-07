@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.atiurin.ultron.core.compose.runUltronUiTest
import com.russhwolf.settings.ObservableSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.usecase.BuildHomeClockSnapshotUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeGreetingUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeHeaderUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRenderRowsUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRowUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodSectionsUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeSurfaceUiUseCase
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAdConfigUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementsUseCase
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.MarkAnnouncementsReadUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.ResolveCourseStatusUseCase
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import restarhalf.stellar.schedule.ui.viewmodel.HomeViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.time.Clock

/**
 * 首页 Compose UI 测试（Ultron KMP 入口）。
 *
 * VM 配方同 JVM 测试：全真实 UseCase 链，只 mock 端口。
 * 学期开始时间取"本周周一 00:00"，课程 weeks 覆盖 1-25，保证真实时钟下
 * 今日课程必然落入当前周渲染。AnnouncementStore 依赖 ObservableSettings
 * （multiplatform-settings），用 autofill mock 补 put* 写入。
 */
class HomeScreenUiTest {

    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val announcementPort = mock<AnnouncementPort>(MockMode.autofill)
    private val observableSettings = mock<ObservableSettings>(MockMode.autofill) {
        every { putString(any(), any()) } returns Unit
        every { putLong(any(), any()) } returns Unit
    }

    private val coursesFlow = MutableStateFlow<List<Course>>(emptyList())
    private val examsAllFlow = MutableStateFlow<List<Examination>>(emptyList())
    private val examsForUserFlow = MutableStateFlow<List<Examination>>(emptyList())

    private val tz = TimeZone.currentSystemDefault()

    /** 本周周一 00:00（本地时区），保证真实时钟下当前周 = 1 */
    private val termStartMs: Long = run {
        val today = Clock.System.now().toLocalDateTime(tz).date
        val monday = LocalDate.fromEpochDays(
            today.toEpochDays() - (today.dayOfWeek.isoDayNumber - 1),
        )
        monday.atStartOfDayIn(tz).toEpochMilliseconds()
    }

    /** 今日星期（ISO 1-7） */
    private val todayDow: Int =
        Clock.System.now().toLocalDateTime(tz).dayOfWeek.isoDayNumber

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

    private fun makeHomeViewModel(): HomeViewModel {
        every { courseRepository.observeAllCourses() } returns coursesFlow
        every { auth.observeProfile() } returns MutableStateFlow(JwxtAuthProfile())
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

    private fun makeAnnouncementViewModel() = AnnouncementViewModel(
        fetchAnnouncements = FetchAnnouncementsUseCase(
            announcementPort, AnnouncementStore(observableSettings),
        ),
        markAnnouncementsRead = MarkAnnouncementsReadUseCase(
            AnnouncementStore(observableSettings),
        ),
        fetchAdConfig = FetchAdConfigUseCase(announcementPort),
        fetchAnnouncement = FetchAnnouncementUseCase(announcementPort),
    )

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 10_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染今日课程与公告入口() = runUltronUiTest {
        // 今日课程：落在当前周（weeks 1-25），星期取设备真实"今天"
        coursesFlow.value = listOf(
            Course(
                name = "高等数学",
                location = "一教101",
                teacher = "张三",
                dayOfWeek = todayDow,
                startSection = 1,
                sectionCount = 2,
                weeks = (1..25).toList(),
                color = "#FF5722",
            ),
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                HomeScreen(
                    vm = makeHomeViewModel(),
                    announcementVm = makeAnnouncementViewModel(),
                    onAnnouncementClick = {},
                    hasBackground = false,
                    componentsAlpha = 1f,
                    campus = Campus.Jinshitan,
                    termStartMs = termStartMs,
                    totalWeeks = 25,
                )
            }
        }
        // 今日课程行渲染课程名
        waitText("高等数学")
        // 右上角公告入口
        onNode(hasContentDescription("公告")).assertExists()
    }

    @Test
    fun 点击公告入口触发回调() = runUltronUiTest {
        var clicked = false
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                HomeScreen(
                    vm = makeHomeViewModel(),
                    announcementVm = makeAnnouncementViewModel(),
                    onAnnouncementClick = { clicked = true },
                    hasBackground = false,
                    componentsAlpha = 1f,
                    campus = Campus.Jinshitan,
                    termStartMs = termStartMs,
                    totalWeeks = 25,
                )
            }
        }
        onNode(hasContentDescription("公告")).performSemanticsAction(SemanticsActions.OnClick)
        waitUntil(timeoutMillis = 5_000) { clicked }
        assertEquals(true, clicked)
    }
}
