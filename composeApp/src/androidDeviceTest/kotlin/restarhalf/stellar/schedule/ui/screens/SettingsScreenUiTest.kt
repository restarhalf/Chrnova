@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.RemoveAllCalendarEventsUseCase
import restarhalf.stellar.schedule.domain.usecase.SyncCourseEventsToCalendarUseCase
import restarhalf.stellar.schedule.domain.usecase.SyncExamEventsToCalendarUseCase
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 设置大屏 Compose UI 测试（Ultron KMP 入口）。
 *
 * VM 配方同 JVM 测试（SettingsViewModelTest）：全部 observe 流用类级
 * MutableStateFlow stub，5 个 final UseCase 走真实实例。
 * TimetablePort 经 koinInject 获取：测试内 startKoin / loadKoinModules 补注册。
 * LazyColumn 只组可见项，只断言顶部账号分区；弹层（日期/周数选择器）为
 * 独立窗口，只断言触发入口。
 */
class SettingsScreenUiTest {

    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val calendarEvent = mock<CalendarEventPort>(MockMode.autofill)
    private val examRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val papersPort = mock<PapersPort>(MockMode.autofill)

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

    private companion object {
        @Volatile
        private var koinReady = false
    }

    private fun ensureKoin() {
        if (koinReady) return
        val module = module { single<TimetablePort> { timetable } }
        if (GlobalContext.getOrNull() == null) {
            startKoin { modules(module) }
        } else {
            loadKoinModules(module)
        }
        koinReady = true
    }

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
        every { settings.getUserAvatarUri() } returns null
        every { settings.getUserNickname() } returns null
        every { auth.observeToken() } returns tokenFlow
        every { auth.observeProfile() } returns profileFlow
        return SettingsViewModel(
            auth = auth,
            authWorkflow = authWorkflow,
            settings = settings,
            syncCourseEventsToCalendar = SyncCourseEventsToCalendarUseCase(
                courseRepository, timetable, calendarEvent, settings,
            ),
            syncExamEventsToCalendar = SyncExamEventsToCalendarUseCase(
                ObserveAllExaminationsUseCase(examRepository, auth), calendarEvent, settings,
            ),
            removeAllCalendarEvents = RemoveAllCalendarEventsUseCase(calendarEvent),
            fetchSemesterIds = FetchSemesterIdsUseCase(authWorkflow, academic, settings),
            verifyGitHubStar = VerifyGitHubStarUseCase(papersPort, settings),
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 10_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染设置分区与登录入口() = runUltronUiTest {
        ensureKoin()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                SettingsScreen(
                    vm = makeViewModel(),
                    syncUiState = SyncUiState.Idle,
                    campus = Campus.Jinshitan,
                    termStartMs = 1_775_000_000_000L,
                    totalWeeks = 20,
                    onSync = {},
                    onLogout = {},
                    onLogin = {},
                    onCampusChange = {},
                    onTermStartChange = {},
                    onTotalWeeksChange = {},
                    onChangeBackground = {},
                    onAbout = {},
                    onPaper = {},
                )
            }
        }
        // 顶栏与账号分区（未登录态）
        waitText("课程表设置")
        waitText("账号")
        waitText("登录教务系统")
    }

    @Test
    fun 点击登录入口触发回调() = runUltronUiTest {
        ensureKoin()
        var loginClicked = false
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                SettingsScreen(
                    vm = makeViewModel(),
                    syncUiState = SyncUiState.Idle,
                    campus = Campus.Jinshitan,
                    termStartMs = 1_775_000_000_000L,
                    totalWeeks = 20,
                    onSync = {},
                    onLogout = {},
                    onLogin = { loginClicked = true },
                    onCampusChange = {},
                    onTermStartChange = {},
                    onTotalWeeksChange = {},
                    onChangeBackground = {},
                    onAbout = {},
                    onPaper = {},
                )
            }
        }
        waitText("登录教务系统")
        // ArrowPreference 的点击经语义 OnClick 直调，避免触碰注入竞态
        onAllNodesWithText("登录教务系统")[0]
            .performSemanticsAction(SemanticsActions.OnClick)
        waitUntil(timeoutMillis = 5_000) { loginClicked }
        assertEquals(true, loginClicked)
    }
}
