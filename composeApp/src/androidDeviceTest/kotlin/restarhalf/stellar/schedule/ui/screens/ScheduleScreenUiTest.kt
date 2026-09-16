@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.atiurin.ultron.core.compose.runUltronUiTest
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
import org.junit.Test
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
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.time.Clock

/**
 * 课程表大屏 Compose UI 测试（Ultron KMP 入口）。
 *
 * VM 配方同 JVM 测试：BuildScheduleUiState/TransCourseWithConflicts/
 * RefreshCourseRemindersIfEnabled 全真实实例，只 mock 端口。
 * 学期开始时间取"本周周一 00:00"——真实时钟下 detectedWeek = 1，
 * 顶栏标题应为"第1周"，网格渲染周一表头与课程卡片。
 */
class ScheduleScreenUiTest {

    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val courseReminder = mock<CourseReminderPort>(MockMode.autofill)

    private val showNonCurrentWeekFlow = MutableStateFlow(true)
    private val rowHeightFlow = MutableStateFlow(SettingsPort.DEFAULT_ROW_HEIGHT_DP)
    private val reminderFlow = MutableStateFlow(false)
    private val coursesFlow = MutableStateFlow<List<Course>>(emptyList())

    private val tz = TimeZone.currentSystemDefault()

    /** 本周周一 00:00（本地时区），真实时钟下当前周 = 1 */
    private val termStartMs: Long = run {
        val today = Clock.System.now().toLocalDateTime(tz).date
        val monday = LocalDate.fromEpochDays(
            today.toEpochDays() - (today.dayOfWeek.isoDayNumber - 1),
        )
        monday.atStartOfDayIn(tz).toEpochMilliseconds()
    }

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

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 10_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染周次标题表头与课程卡片() = runUltronUiTest {
        // 当前周（第1周）课程：weeks 覆盖 1-25，周一 1-2 节
        coursesFlow.value = listOf(
            Course(
                name = "高等数学",
                location = "一教101",
                teacher = "张三",
                dayOfWeek = 1,
                startSection = 1,
                sectionCount = 2,
                weeks = (1..25).toList(),
                color = "#FF5722",
            ),
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ScheduleScreen(
                    vm = makeViewModel(),
                    onSync = {},
                    syncUiState = SyncUiState.Idle,
                    campus = Campus.Jinshitan,
                    termStartMs = termStartMs,
                    totalWeeks = 25,
                    onAddLabCourse = { _, _, _ -> },
                    onEditLabCourse = {},
                )
            }
        }
        // 顶栏周次标题（currentWeek=1）
        waitText("第1周")
        // 网格周表头
        onNodeWithText("周一").assertExists()
        onNodeWithText("周日").assertExists()
        // 课程卡片渲染课程名
        waitText("高等数学")
    }

    @Test
    fun 空课表渲染不崩溃() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ScheduleScreen(
                    vm = makeViewModel(),
                    onSync = {},
                    syncUiState = SyncUiState.Idle,
                    campus = Campus.Jinshitan,
                    termStartMs = termStartMs,
                    totalWeeks = 25,
                    onAddLabCourse = { _, _, _ -> },
                    onEditLabCourse = {},
                )
            }
        }
        waitText("第1周")
        onNodeWithText("周一").assertExists()
    }
}
