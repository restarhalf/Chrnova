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
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.LogSettingsPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.port.UserProfilePort
import restarhalf.stellar.schedule.domain.port.TimetablePort
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
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class SettingsScreenUiTest {

    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val displaySettings = mock<DisplaySettingsPort>(MockMode.autofill)
    private val reminderSettings = mock<ReminderSettingsPort>(MockMode.autofill)
    private val termSettings = mock<TermSettingsPort>(MockMode.autofill)
    private val logSettings = mock<LogSettingsPort>(MockMode.autofill)
    private val userProfile = mock<UserProfilePort>(MockMode.autofill)
    private val deviceSettings = mock<DeviceSettingsPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val courseReminder = mock<CourseReminderPort>(MockMode.autofill)
    private val examReminder = mock<ExamReminderPort>(MockMode.autofill)
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
        every { displaySettings.observeShowNonCurrentWeek() } returns showFlow
        every { reminderSettings.observeCourseReminderEnabled() } returns reminderFlow
        every { reminderSettings.observeExamReminderEnabled() } returns examReminderFlow
        every { displaySettings.observeThemeMode() } returns themeFlow
        every { displaySettings.observeFloatingBar() } returns floatingBarFlow
        every { termSettings.observeSelectedTerm() } returns termFlow
        every { logSettings.observeLogEnabled() } returns logFlow
        every { displaySettings.observeScheduleRowHeight() } returns rowHeightFlow
        every { termSettings.observeCachedSemesterIds() } returns flowOf(emptyList())
        every { deviceSettings.getStarVerified() } returns false
        every { userProfile.getUserAvatarUri() } returns null
        every { userProfile.getUserNickname() } returns null
        every { auth.observeToken() } returns tokenFlow
        every { auth.observeProfile() } returns profileFlow
        return SettingsViewModel(
            auth = auth,
            authWorkflow = authWorkflow,
            displaySettings = displaySettings,
            reminderSettings = reminderSettings,
            termSettings = termSettings,
            logSettings = logSettings,
            userProfile = userProfile,
            deviceSettings = deviceSettings,
            cancelAllCourseReminders = CancelAllCourseRemindersUseCase(courseReminder),
            cancelAllExamReminders = CancelAllExamRemindersUseCase(examReminder),
            fetchSemesterIds = FetchSemesterIdsUseCase(authWorkflow, academic, termSettings),
            scheduleNextCourseReminder = ScheduleNextCourseReminderUseCase(
                courseRepository, courseReminder,
            ),
            scheduleNextExamReminder = ScheduleNextExamReminderUseCase(
                FetchExaminationsUseCase(authWorkflow, academic, examRepository, auth, termSettings),
                examReminder,
            ),
            verifyGitHubStar = VerifyGitHubStarUseCase(papersPort, deviceSettings),
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

        onAllNodesWithText("登录教务系统")[0]
            .performSemanticsAction(SemanticsActions.OnClick)
        waitUntil(timeoutMillis = 5_000) { loginClicked }
        assertEquals(true, loginClicked)
    }
}
