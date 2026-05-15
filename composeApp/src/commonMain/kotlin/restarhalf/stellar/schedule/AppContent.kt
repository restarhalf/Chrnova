package restarhalf.stellar.schedule

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import coil3.compose.AsyncImage
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.ui.image.toAsyncImageModel
import restarhalf.stellar.schedule.ui.navigation.AppBottomBar
import restarhalf.stellar.schedule.ui.navigation.AppChromeState
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.AppScaffoldBody
import restarhalf.stellar.schedule.ui.navigation.LocalAppChromeState
import restarhalf.stellar.schedule.ui.navigation.LocalMainPagerState
import restarhalf.stellar.schedule.ui.navigation.LocalNavigator
import restarhalf.stellar.schedule.ui.navigation.RootTabs
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.navigation.rememberMainPagerState
import restarhalf.stellar.schedule.ui.navigation.rootTabAt
import restarhalf.stellar.schedule.ui.screens.AboutScreen
import restarhalf.stellar.schedule.ui.screens.AgentScreen
import restarhalf.stellar.schedule.ui.screens.ChangeBackgroundScreen
import restarhalf.stellar.schedule.ui.screens.CourseEditScreen
import restarhalf.stellar.schedule.ui.screens.ExaminationScreen
import restarhalf.stellar.schedule.ui.screens.GradeScreen
import restarhalf.stellar.schedule.ui.screens.HomeScreen
import restarhalf.stellar.schedule.ui.screens.ScheduleScreen
import restarhalf.stellar.schedule.ui.screens.SettingsScreen
import restarhalf.stellar.schedule.ui.screens.SettingsScreenActions
import restarhalf.stellar.schedule.ui.screens.SettingsScreenState
import restarhalf.stellar.schedule.ui.viewmodel.AboutUiEvent
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppContent(
    vm: AppViewModel,
    bgVm: BackgroundViewModel,
    appUpdate: AppUpdatePort,
    appIcon: ImageBitmap? = null,
    pictureSelectorHost: @Composable (
        show: Boolean,
        onDismissRequest: () -> Unit,
        onPicked: (String) -> Unit,
    ) -> Unit = { _, _, _ -> },
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
    openUri: (String) -> Boolean = { false },
    showMessage: (String) -> Unit = {},
    canSaveAwardPicture: Boolean = false,
    saveAwardPicture: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    runSync: suspend () -> Unit,
) {
    val appState = LocalAppState.current

    val backgroundUiState by bgVm.uiState.collectAsState()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop{
        drawRect(surfaceColor)
        drawContent()
    }

    val pagerState = rememberPagerState(pageCount = { RootTabs.size })
    val mainPagerState = rememberMainPagerState(pagerState)
    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    val backStack = remember { mutableStateListOf<NavKey>(Screen.Main) }
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val currentRoute = (navigator.current() as? Screen) ?: Screen.Main
    val chromeState =
        remember(
            currentRoute,
            mainPagerState.selectedPage,
            appState.isWideScreen,
            appState.barMode,
        ) {
            AppChromeState(
                currentScreen = mainPagerState.currentScreen,
                isMainRoute = currentRoute == Screen.Main,
                isWideScreen = appState.isWideScreen,
                barMode = appState.barMode,
            )
        }

    val entryProvider =
        remember(
            backStack,
            appIcon,
            pictureSelectorHost,
            ensureNotificationPermission,
            openUri,
            showMessage,
            canSaveAwardPicture,
            saveAwardPicture,
            appUpdate,
            vm,
            runSync,
        ) {
            entryProvider<NavKey> {
                entry(Screen.Main) {
                    MainRouteContent(
                        vm = vm,
                        runSync = runSync,
                        ensureNotificationPermission = ensureNotificationPermission,
                    )
                }
                entry(Screen.ChangeBackground) {
                    ChangeBackgroundScreen(
                        onBack = { navigator.pop() },
                        pictureSelectorHost = pictureSelectorHost,
                    )
                }
                entry(Screen.About) {
                    AboutScreen(
                        onBack = { navigator.pop() },
                        appIcon = appIcon,
                        showMessage = showMessage,
                        canSaveAwardPicture = canSaveAwardPicture,
                        onSaveAwardPicture = saveAwardPicture,
                        onHandleEvent = { event ->
                            when (event) {
                                is AboutUiEvent.OpenUri -> {
                                    if (!openUri(event.uri)) {
                                        showMessage("无法打开链接")
                                    }
                                }

                                is AboutUiEvent.JoinQqGroup -> {
                                    if (!appUpdate.joinQqGroup(key = event.key)) {
                                        showMessage("请检查是否安装了 QQ")
                                    }
                                }

                                AboutUiEvent.WxPayAwardRequested -> {
                                    val saved = appUpdate.saveWxpayToPictures()
                                    showMessage(
                                        if (saved) {
                                            "赞赏码已保存到相册，即将跳转到微信扫一扫"
                                        } else {
                                            "保存赞赏码失败，请重试"
                                        },
                                    )
                                    if (!appUpdate.openWeChatScanDirect()) {
                                        showMessage("微信启动失败，请检查是否安装微信")
                                    }
                                }
                            }
                        },
                        onStartDownload = { info -> appUpdate.startDirectDownload(info) },
                    )
                }
                entry<Screen.ClassEdit> { screen ->
                    val isEdit = remember(screen.courseId) { mutableStateOf(screen.courseId != null) }
                    CourseEditScreen(
                        onBack = { navigator.pop() },
                        isEdit = isEdit,
                        courseId = screen.courseId,
                    )
                }
                entry<Screen.Agent> {
                    AgentScreen(
                        onBack = { navigator.pop() },
                    )
                }
            }
        }

    val entries =
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider = entryProvider,
        )
    val transitionEffects =
        remember(
            appState.enableCornerClip,
            appState.blockInputDuringTransition,
            appState.popDirectionFollowsSwipeEdge,
        ) {
            NavDisplayTransitionEffects(
                enableCornerClip = appState.enableCornerClip,
                dimAmount = 0f,
                blockInputDuringTransition = appState.blockInputDuringTransition,
                popDirectionFollowsSwipeEdge = appState.popDirectionFollowsSwipeEdge,
            )
        }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalAppChromeState provides chromeState,
        LocalMainPagerState provides mainPagerState,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface),
        ) {
            Scaffold(
                containerColor = MiuixTheme.colorScheme.background.copy(alpha = 0f),
                bottomBar = {
                    AppBottomBar(backdrop = backdrop)
                },
            ) { innerPadding ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                            .background(MiuixTheme.colorScheme.surface),
                ) {
                    backgroundUiState.backgroundImageUri?.let { uri ->
                        AsyncImage(
                            model = toAsyncImageModel(uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .blur(25.dp * backgroundUiState.backgroundBlur)
                                    .alpha(backgroundUiState.backgroundAlpha),
                        )
                    }

                    AppScaffoldBody(
                        innerPadding = innerPadding,
                        componentsAlpha = backgroundUiState.componentsAlpha,
                    ) {
                        NavDisplay(
                            entries = entries,
                            onBack = { navigator.pop() },
                            transitionEffects = transitionEffects,
                            transitionSpec = {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth / 3 },
                                    animationSpec = tween(durationMillis = 260),
                                ) + fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> -fullWidth / 10 },
                                        animationSpec = tween(durationMillis = 260),
                                    ) + fadeOut(animationSpec = tween(durationMillis = 220))
                            },
                            popTransitionSpec = {
                                slideInHorizontally(
                                    initialOffsetX = { fullWidth -> -fullWidth / 5 },
                                    animationSpec = tween(durationMillis = 240),
                                ) + fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { fullWidth -> fullWidth / 3 },
                                        animationSpec = tween(durationMillis = 240),
                                    ) + fadeOut(animationSpec = tween(durationMillis = 200))
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainRouteContent(
    vm: AppViewModel,
    runSync: suspend () -> Unit,
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit,
) {
    val appState = LocalAppState.current
    val updateAppState = LocalUpdateAppState.current
    val navigator = LocalNavigator.current
    val mainPagerState = LocalMainPagerState.current

    HorizontalPager(
        state = mainPagerState.pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = appState.enablePageUserScroll,
        verticalAlignment = Alignment.Top,
    ) { page ->
        when (rootTabAt(page)) {
            Screen.Home -> {
                HomeScreen(
                    campus = appState.campus,
                    termStartMs = appState.termStartMs,
                    totalWeeks = appState.totalWeeks,
                    onAgent = { navigator.push(Screen.Agent) },
                )
            }

            Screen.Schedule -> {
                ScheduleScreen(
                    onSync = runSync,
                    campus = appState.campus,
                    termStartMs = appState.termStartMs,
                    totalWeeks = appState.totalWeeks,
                    onAddLabCourse = { navigator.push(Screen.ClassEdit()) },
                    onEditLabCourse = { courseId ->
                        navigator.push(Screen.ClassEdit(courseId = courseId))
                    },
                )
            }

            Screen.Examination -> {
                ExaminationScreen(onLoadExaminations = { vm.fetchExaminationArrangements() })
            }

            Screen.Grade -> {
                GradeScreen(onLoadGrades = { vm.fetchGradeReport() })
            }

            Screen.Settings -> {
                SettingsScreen(
                    state =
                        SettingsScreenState(
                            syncUiState = appState.syncUiState,
                            campus = appState.campus,
                            termStartMs = appState.termStartMs,
                            totalWeeks = appState.totalWeeks,
                        ),
                    actions =
                        SettingsScreenActions(
                            onSync = runSync,
                            onLogout = { vm.logout() },
                            onLogin = { userNo, password ->
                                vm.login(userNo = userNo, password = password)
                            },
                            ensureCourseReminderPermission = ensureNotificationPermission,
                            ensureExamReminderPermission = ensureNotificationPermission,
                            onCampusChange = { campus ->
                                updateAppState { current -> current.copy(campus = campus) }
                                vm.onCampusChanged(campus)
                            },
                            onTermStartChange = { termStartMs ->
                                updateAppState { current -> current.copy(termStartMs = termStartMs) }
                                vm.onTermStartMsChanged(termStartMs)
                            },
                            onTotalWeeksChange = { totalWeeks ->
                                updateAppState { current -> current.copy(totalWeeks = totalWeeks) }
                                vm.onTotalWeeksChanged(totalWeeks)
                            },
                            onChangeBackground = { navigator.push(Screen.ChangeBackground) },
                            onAbout = { navigator.push(Screen.About) },
                        ),
                )
            }

            else -> Unit
        }
    }
}
