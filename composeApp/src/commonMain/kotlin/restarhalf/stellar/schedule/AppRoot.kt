package restarhalf.stellar.schedule

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.ui.NavDisplay
import coil3.compose.AsyncImage
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.core.update.ApkDownloadState
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.ui.components.screen.about.DownloadDialog
import restarhalf.stellar.schedule.ui.components.screen.about.UpdateConfirmDialog
import restarhalf.stellar.schedule.ui.image.toAsyncImageModel
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.AppBottomBar
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.AppScaffoldBody
import restarhalf.stellar.schedule.ui.navigation.FirstOpenNoticeDialog
import restarhalf.stellar.schedule.ui.navigation.LocalGlassNavigationBackdrop
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.navigation.rememberAppShellState
import restarhalf.stellar.schedule.ui.navigation.shouldShowSplitPane
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.screens.AboutScreen
import restarhalf.stellar.schedule.ui.screens.ChangeBackgroundScreen
import restarhalf.stellar.schedule.ui.screens.CourseEditScreen
import restarhalf.stellar.schedule.ui.screens.ExaminationScreen
import restarhalf.stellar.schedule.ui.screens.GradeScreen
import restarhalf.stellar.schedule.ui.screens.HomeScreen
import restarhalf.stellar.schedule.ui.screens.ScheduleScreen
import restarhalf.stellar.schedule.ui.screens.SettingsScreen
import restarhalf.stellar.schedule.ui.screens.SettingsScreenActions
import restarhalf.stellar.schedule.ui.screens.SettingsScreenState
import restarhalf.stellar.schedule.ui.sync.SyncUiState
import restarhalf.stellar.schedule.ui.viewmodel.AboutUiEvent
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppRoot(
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
    exitApp: () -> Unit = {},
) {
    val vm: AppViewModel = koinViewModel()
    val bgVm: BackgroundViewModel = koinViewModel()
    val appUpdate: AppUpdatePort = koinInject()
    val appInfo: AppInfoPort = koinInject()
    val settings: ObservableSettings = koinInject(named(SettingsKeys.PREFS_NAME))
    val isWideScreen = shouldShowSplitPane()
    val isAndroidPlatform = remember { getPlatform().name.startsWith("Android") }

    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    val showUpdateDialog = remember { mutableStateOf(false) }
    val showFirstOpenDialog = remember { mutableStateOf(false) }

    val backgroundImageUri by bgVm.backgroundImageUri.collectAsState()
    val backgroundAlpha by bgVm.backgroundAlpha.collectAsState()
    val backgroundBlur by bgVm.backgroundBlur.collectAsState()
    val componentsAlpha by bgVm.componentsAlpha.collectAsState()
    val surfaceColor = MiuixTheme.colorScheme.surface
    val appBackdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    var campus by remember { mutableStateOf(vm.getCampus()) }
    var termStartMs by remember { mutableLongStateOf(vm.getTermStartMs()) }
    var totalWeeks by remember { mutableIntStateOf(vm.getTotalWeeks()) }
    var syncUiState by remember { mutableStateOf<SyncUiState>(SyncUiState.Idle) }

    val runSync = remember(vm) {
        suspend {
            vm.runSync { state ->
                syncUiState = state
            }
        }
    }

    val backStack = remember { mutableStateListOf<NavKey>(Screen.Home) }
    val navigator = remember(backStack) { AppNavigator(backStack) }

    LaunchedEffect(Unit) {
        val hasShown = settings.getBoolean(SettingsKeys.HAS_SHOWN_FIRST_OPEN_DIALOG, false)
        if (!hasShown) {
            settings[SettingsKeys.HAS_SHOWN_FIRST_OPEN_DIALOG] = true
            showFirstOpenDialog.value = true
        }
    }

    LaunchedEffect(Unit) {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val lastCheck = settings.getLong(SettingsKeys.LAST_UPDATE_CHECK_MS, 0L)
        val intervalMs = 2L * 24 * 60 * 60 * 1000
        if (now - lastCheck < intervalMs) return@LaunchedEffect

        settings[SettingsKeys.LAST_UPDATE_CHECK_MS] = now

        val latest =
            runCatching { appUpdate.check(currentVersionName = appInfo.versionName) }.getOrNull()

        if (latest != null) {
            pendingUpdate = latest
            showUpdateDialog.value = true
        }
    }

    fun switchTab(target: Screen) {
        navigator.replaceRoot(target)
    }

    val entryProvider =
        remember(backStack, campus, termStartMs, totalWeeks, syncUiState) {
            entryProvider<NavKey> {
                entry(Screen.Home) {
                    HomeScreen(
                        campus = campus,
                        termStartMs = termStartMs,
                        totalWeeks = totalWeeks,
                    )
                }
                entry(Screen.Schedule) {
                    ScheduleScreen(
                        onSync = runSync,
                        campus = campus,
                        termStartMs = termStartMs,
                        totalWeeks = totalWeeks,
                        onAddLabCourse = { navigator.push(Screen.ClassEdit()) },
                        onEditLabCourse = { courseId ->
                            navigator.push(Screen.ClassEdit(courseId = courseId))
                        },
                    )
                }
                entry(Screen.Examination) {
                    ExaminationScreen(onLoadExaminations = { vm.fetchExaminationArrangements() })
                }
                entry(Screen.Grade) {
                    GradeScreen(onLoadGrades = { vm.fetchGradeReport() })
                }
                entry(Screen.Settings) {
                    SettingsScreen(
                        state =
                            SettingsScreenState(
                                syncUiState = syncUiState,
                                campus = campus,
                                termStartMs = termStartMs,
                                totalWeeks = totalWeeks,
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
                                onCampusChange = {
                                    campus = it
                                    vm.setCampus(it)
                                },
                                onTermStartChange = {
                                    termStartMs = it
                                    vm.setTermStartMs(it)
                                },
                                onTotalWeeksChange = {
                                    totalWeeks = it
                                    vm.setTotalWeeks(it)
                                },
                                onChangeBackground = { navigator.push(Screen.ChangeBackground) },
                                onAbout = { navigator.push(Screen.About) },
                            ),
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
                                        }
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
                    val isEdit =
                        remember(screen.courseId) { mutableStateOf(screen.courseId != null) }
                    CourseEditScreen(
                        onBack = { navigator.pop() },
                        isEdit = isEdit,
                        courseId = screen.courseId,
                    )
                }
            }
        }

    val entries =
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = emptyList(),
            entryProvider = entryProvider,
        )

    val currentScreen = navigator.current() as? Screen
    val shellState =
        rememberAppShellState(
            currentScreen = if (navigator.backStackSize() == 1) currentScreen else null,
            isWideScreen = isWideScreen,
            settings = settings,
        )

    CompositionLocalProvider(LocalGlassNavigationBackdrop provides appBackdrop) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface)
        ) {
            Scaffold(
                containerColor = MiuixTheme.colorScheme.background.copy(alpha = 0f),
                bottomBar = {
                    AppBottomBar(shellState = shellState, onSwitchTab = ::switchTab)
                },
            ) { innerPadding ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .layerBackdrop(appBackdrop)
                            .background(MiuixTheme.colorScheme.surface),
                ) {
                    backgroundImageUri?.let { uri ->
                        AsyncImage(
                            model = toAsyncImageModel(uri),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .blur(25.dp * backgroundBlur)
                                    .alpha(backgroundAlpha),
                        )
                    }

                    AppScaffoldBody(
                        shellState = shellState,
                        innerPadding = innerPadding,
                        componentsAlpha = componentsAlpha,
                        onSwitchTab = ::switchTab,
                    ) {
                        NavDisplay(
                            entries = entries,
                            onBack = { navigator.pop() },
                            transitionSpec = {
                                fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                        fadeOut(animationSpec = tween(durationMillis = 150))
                            },
                            popTransitionSpec = {
                                fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 150))
                            },
                        )
                    }
                }
            }
        }
    }

    if (showUpdateDialog.value) {
        UpdateConfirmDialog(
            show = showUpdateDialog,
            pendingUpdate = pendingUpdate,
            onStartDownload = { info ->
                if (!isAndroidPlatform) {
                    val openedDownload =
                        runCatching { appUpdate.launchInstaller(info.downloadUrl) }.getOrDefault(
                            false
                        )
                    val openedReleasePage =
                        if (openedDownload) true
                        else runCatching { appUpdate.launchInstaller(info.releasePageUrl) }.getOrDefault(
                            false
                        )
                    showMessage(
                        if (openedReleasePage) "已打开下载链接，请在浏览器完成安装"
                        else "无法打开下载链接，请稍后重试"
                    )
                    return@UpdateConfirmDialog
                }
                appUpdate.startDirectDownload(info)
            },
        )
    }

    val apkDownloadState by appUpdate.apkDownloadState.collectAsState()
    val showApkDownloadDialog = remember { mutableStateOf(false) }

    LaunchedEffect(apkDownloadState, isAndroidPlatform) {
        if (!isAndroidPlatform) return@LaunchedEffect
        when (val state = apkDownloadState) {
            is ApkDownloadState.Downloading -> {
                showApkDownloadDialog.value = true
            }

            is ApkDownloadState.Completed -> {
                if (!appUpdate.canRequestInstallPackages()) {
                    showMessage("请允许安装未知应用后重试安装")
                    appUpdate.openUnknownSourcesSettings()
                } else {
                    val launched =
                        runCatching { appUpdate.launchInstaller(state.filePath) }.getOrDefault(false)
                    if (!launched) {
                        showMessage("无法拉起安装，请在文件管理器中手动安装")
                    }
                }
                showApkDownloadDialog.value = false
            }

            is ApkDownloadState.Error -> {
                showMessage(state.message)
                showApkDownloadDialog.value = false
            }

            ApkDownloadState.Idle -> {
                showApkDownloadDialog.value = false
            }
        }
    }

    if (isAndroidPlatform && showApkDownloadDialog.value) {
        val progress =
            (apkDownloadState as? ApkDownloadState.Downloading)?.progress
                ?: if (apkDownloadState is ApkDownloadState.Completed) 100f else 0f

        DownloadDialog(
            show = showApkDownloadDialog,
            downloadProgress = progress,
            onStop = {
                appUpdate.cancelApkDownload()
                showApkDownloadDialog.value = false
            },
            onBackGround = { showApkDownloadDialog.value = false },
        )
    }

    if (showFirstOpenDialog.value) {
        FirstOpenNoticeDialog(
            show = showFirstOpenDialog.value,
            onDismiss = { showFirstOpenDialog.value = false },
            onExit = {
                settings[SettingsKeys.HAS_SHOWN_FIRST_OPEN_DIALOG] = false
                exitApp()
            },
        )
    }
}
