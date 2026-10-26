package restarhalf.stellar.schedule.ui.screens.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.LocalMainPagerState
import restarhalf.stellar.schedule.ui.navigation.LocalNavigator
import restarhalf.stellar.schedule.ui.navigation.MainPagerState
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.navigation.rootTabAt
import restarhalf.stellar.schedule.ui.screens.EMSScreen
import restarhalf.stellar.schedule.ui.screens.HomeScreen
import restarhalf.stellar.schedule.ui.screens.ScheduleScreen
import restarhalf.stellar.schedule.ui.screens.SettingsScreen
import restarhalf.stellar.schedule.ui.screens.pe.PEScoreScreen
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.HomeViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PersonalInfoViewModel
import restarhalf.stellar.schedule.ui.viewmodel.ScheduleViewModel
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainRouteContent(
    bgVm: BackgroundViewModel,
    vm: AppViewModel,
    announcementVm: AnnouncementViewModel,
    runSync: suspend () -> Unit,
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit,
    saveCsv: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    showMessage: (String) -> Unit = {},
    canSaveImage: Boolean = false,
    saveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
) {
    val homeVm: HomeViewModel = koinViewModel()
    val scheduleVm: ScheduleViewModel = koinViewModel()
    val examVm: ExaminationViewModel = koinViewModel()
    val gradeVm: GradeViewModel = koinViewModel()
    val peVm: PEViewModel = koinViewModel()
    val settingsVm: SettingsViewModel = koinViewModel()
    val appUiState by vm.uiState.collectAsStateWithLifecycle()
    val syncUiState by vm.syncUiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val mainPagerState = LocalMainPagerState.current

    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        if (rootTabAt(mainPagerState.pagerState.currentPage) == Screen.Home) {
            announcementVm.refresh()
        }
    }

    HorizontalPager(
        state = mainPagerState.pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = true,
        verticalAlignment = Alignment.Top,
    ) { page ->
        when (rootTabAt(page)) {
            Screen.Home -> {
                val bgUiState by bgVm.uiState.collectAsStateWithLifecycle()
                HomeScreen(
                    vm = homeVm,
                    announcementVm = announcementVm,
                    onAnnouncementClick = { navigator.push(Screen.AnnouncementList) },
                    campus = appUiState.campus,
                    termStartMs = appUiState.termStartMs,
                    totalWeeks = appUiState.totalWeeks,
                    componentsAlpha = bgUiState.componentsAlpha,
                    hasBackground = bgUiState.backgroundImageUri != null
                )
            }

            Screen.Schedule -> {
                val personalInfoVm: PersonalInfoViewModel = koinViewModel()
                val personalInfo by personalInfoVm.uiState.collectAsStateWithLifecycle()
                val bgState by bgVm.uiState.collectAsStateWithLifecycle()
                ScheduleScreen(
                    vm = scheduleVm,
                    onSync = runSync,
                    syncUiState = syncUiState,
                    showMessage = showMessage,
                    onSyncErrorConsumed = { vm.acknowledgeSyncResult() },
                    campus = appUiState.campus,
                    termStartMs = appUiState.termStartMs,
                    totalWeeks = appUiState.totalWeeks,
                    onAddLabCourse = { dayOfWeek, startSection, selectedWeek ->
                        navigator.push(Screen.ClassEdit(dayOfWeek = dayOfWeek, startSection = startSection, selectedWeek = selectedWeek))
                    },
                    onEditLabCourse = { courseId ->
                        navigator.push(Screen.ClassEdit(courseId = courseId))
                    },
                    canSaveImage = canSaveImage,
                    onSaveImage = saveImage,
                    sharerNickname = personalInfo.nickname,
                    sharerAvatarUri = personalInfo.avatarUri,
                    backgroundImageUri = bgState.backgroundImageUri,
                    backgroundAlpha = bgState.backgroundAlpha,
                    backgroundBlur = bgState.backgroundBlur,
                )
            }

            Screen.EMS -> {
                EMSScreen(
                    examVm = examVm,
                    gradeVm = gradeVm,
                    onLoadExaminations = { vm.fetchExaminationArrangements() },
                    onLoadGrades = { vm.fetchGradeReport() },
                    onAddExam = { navigator.push(Screen.ExamEdit()) },
                    onEditExam = { examId -> navigator.push(Screen.ExamEdit(examinationId = examId)) },
                    onNavigateToElectiveCredit = { navigator.push(Screen.ElectiveCredit) },
                )
            }

            Screen.PEScore -> {
                val settingsUiState by settingsVm.uiState.collectAsStateWithLifecycle()
                PEScoreScreen(
                    vm = peVm,
                    onNavigateToDetail = { schoolYear ->
                        navigator.push(Screen.PEDetail(schoolYear))
                    },
                    onQRCode = { navigator.push(Screen.PEQRCode) },
                    onLogin = { navigator.push(Screen.PELogin) },
                    onAppointment = { navigator.push(Screen.PEAppointment) },
                    onFreeApply = { navigator.push(Screen.PEFreeApply) },
                    jwxtAuthProfile = settingsUiState.profile
                )
            }

            Screen.Settings -> {
                val isPeLoggedIn by peVm.isLoggedIn.collectAsStateWithLifecycle()

                val courses by scheduleVm.allCourses.collectAsStateWithLifecycle()
                SettingsScreen(
                    vm = settingsVm,
                    syncUiState = syncUiState,
                    campus = appUiState.campus,
                    termStartMs = appUiState.termStartMs,
                    totalWeeks = appUiState.totalWeeks,
                    onSync = runSync,
                    onLogout = { vm.logout() },
                    ensureCourseReminderPermission = ensureNotificationPermission,
                    ensureExamReminderPermission = ensureNotificationPermission,
                    onCampusChange = { campus -> vm.onCampusChanged(campus) },
                    onTermStartChange = { termStartMs -> vm.onTermStartMsChanged(termStartMs) },
                    onTotalWeeksChange = { totalWeeks -> vm.onTotalWeeksChanged(totalWeeks) },
                    onChangeBackground = { navigator.push(Screen.ChangeBackground) },
                    onAbout = { navigator.push(Screen.About) },
                    onPaper = { navigator.push(Screen.Papers) },
                    onEvaluation = { navigator.push(Screen.Evaluation) },
                    onCourseSelection = { navigator.push(Screen.CourseSelection) },
                    onCourseEnroll = { navigator.push(Screen.CourseEnroll) },
                    onLogin = { navigator.push(Screen.JwxtLogin) },
                    onProfile = { navigator.push(Screen.Profile) },
                    isPeLoggedIn = isPeLoggedIn,
                    onExportCsv = saveCsv,
                    courses = courses,
                    showMessage = showMessage,
                )
            }

            else -> Unit
        }
    }
}

@Composable
fun MainScreenBackHandler(
    mainState: MainPagerState,
    navigator: AppNavigator,
) {
    val isPagerBackHandlerEnabled by remember {
        derivedStateOf {
            navigator.current() is Screen.Main &&
                    navigator.backStackSize() == 1 &&
                    mainState.selectedPage != 0
        }
    }

    val navEventState = rememberNavigationEventState(NavigationEventInfo.None)

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = isPagerBackHandlerEnabled,
        onBackCompleted = {
            mainState.animateToPage(0)
        },
    )
}
