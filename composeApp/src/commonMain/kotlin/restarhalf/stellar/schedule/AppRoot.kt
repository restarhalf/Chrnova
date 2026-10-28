package restarhalf.stellar.schedule

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.impl.DauReporter
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.usecase.CheckAppUpdateUseCase
import restarhalf.stellar.schedule.ui.components.screen.about.UpdateConfirmDialog
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.screens.exclusion.WelcomeScreen
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Composable
fun AppRoot(
    pictureSelectorHost: @Composable (
        show: Boolean,
        onDismissRequest: () -> Unit,
        onPicked: (String) -> Unit,
        outputWidthPx: Int?,
        outputHeightPx: Int?,
    ) -> Unit = { _, _, _, _, _ -> },
    pdfFilePickerHost: @Composable (
        multiple: Boolean,
        onPicked: (List<restarhalf.stellar.schedule.papers.PickedAttachment>) -> Unit,
    ) -> Unit = { _, _ -> },
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
    ensureExactAlarmPermission: () -> Unit = {},
    openUri: (String) -> Boolean = { false },
    showMessage: (String) -> Unit = {},
    canSaveAwardPicture: Boolean = false,
    saveAwardPicture: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    saveLog: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    saveCsv: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    canSaveImage: Boolean = false,
    saveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    exitApp: () -> Unit = {},
    onThemeModeChange: (Int) -> Unit = {},
) {

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(coil3.network.ktor3.KtorNetworkFetcherFactory()) }
            .build()
    }

    val vm: AppViewModel = koinViewModel()
    val bgVm: BackgroundViewModel = koinViewModel()
    val appUpdate: AppUpdatePort = koinInject()
    val checkAppUpdate: CheckAppUpdateUseCase = koinInject()
    val appInfo: AppInfoPort = koinInject()
    val displaySettings: DisplaySettingsPort = koinInject()
    val deviceSettings: DeviceSettingsPort = koinInject()
    val reminderSettings: ReminderSettingsPort = koinInject()

    // 提醒开着却没给精确闹钟授权，闹钟会被系统塞进模糊窗口、到点不响。
    // 启动时补一次申请，用户从旧版本升上来也能修好。
    // 只问一次：Android 14 起这个权限默认不给，用户不点头就会每次冷启动都被
    // 拉去系统设置页。问过就记下来，之后想再要可以去设置页关掉提醒再打开。
    LaunchedEffect(Unit) {
        val enabled =
            reminderSettings.observeCourseReminderEnabled().first() ||
                reminderSettings.observeExamReminderEnabled().first()
        if (enabled && !reminderSettings.hasAskedExactAlarmPermission()) {
            ensureExactAlarmPermission()
        }
    }

    var appState by remember {
        mutableStateOf(
            AppState(
                themeMode = displaySettings.getThemeMode(),
                barMode = displaySettings.getFloatingBar(),
            ),
        )
    }

    LaunchedEffect(displaySettings) {
        combine(
            displaySettings.observeThemeMode(),
            displaySettings.observeFloatingBar(),
        ) { themeMode, barMode -> themeMode to barMode }
            .collect { (themeMode, barMode) ->
                val updated = appState.copy(themeMode = themeMode, barMode = barMode)
                if (updated != appState) {
                    appState = updated
                }
            }
    }

    val currentOnThemeModeChange by rememberUpdatedState(onThemeModeChange)
    LaunchedEffect(Unit) {
        snapshotFlow { appState.themeMode }.collect { currentOnThemeModeChange(it) }
    }

    val runSync = remember(vm) {
        suspend { vm.runSync() }
    }

    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val showWelcome = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasShown = deviceSettings.getConfirmPrivacy()
        if (!hasShown) {
            showWelcome.value = true
        }
    }

    LaunchedEffect(Unit) {

        runCatching { checkAppUpdate(currentVersionName = appInfo.versionName) }
            .onSuccess { latest ->
                if (latest != null) {
                    pendingUpdate = latest
                    showUpdateDialog = true
                }
            }
            .onFailure {
                AppLogger.log("Update", "自动检查更新失败", it)
            }
    }

    LaunchedEffect(Unit) {

        runCatching { DauReporter.pingTodayIfDue(deviceSettings = deviceSettings) }
            .onFailure {
                AppLogger.log("Dau", "日活上报失败", it)
            }
    }

    val themeController =
        remember(appState.themeMode) {
            ThemeController(
                colorSchemeMode =
                    when (appState.themeMode) {
                        1 -> ColorSchemeMode.Light
                        2 -> ColorSchemeMode.Dark
                        else -> ColorSchemeMode.System
                    },
            )
        }

    MiuixTheme(controller = themeController) {
        CompositionLocalProvider(
            LocalAppState provides appState,
        ) {
            AppContent(
                vm = vm,
                bgVm = bgVm,
                appUpdate = appUpdate,
                pictureSelectorHost = pictureSelectorHost,
                pdfFilePickerHost = pdfFilePickerHost,
                ensureNotificationPermission = ensureNotificationPermission,
                ensureExactAlarmPermission = ensureExactAlarmPermission,
                openUri = openUri,
                showMessage = showMessage,
                canSaveAwardPicture = canSaveAwardPicture,
                saveAwardPicture = saveAwardPicture,
                saveLog = saveLog,
                saveCsv = saveCsv,
                canSaveImage = canSaveImage,
                saveImage = saveImage,
                runSync = runSync,
            )

            if (showUpdateDialog) {
                UpdateConfirmDialog(
                    show = showUpdateDialog,
                    onDismissRequest = { showUpdateDialog = false },
                    pendingUpdate = pendingUpdate,
                    onStartDownload = { info ->
                        appUpdate.startDirectDownload(info)
                    },
                )
            }

            if (showWelcome.value) {
                val welcomePagerState = rememberPagerState(pageCount = { 6 })
                WelcomeScreen(
                    show = showWelcome,
                    pagerState = welcomePagerState,
                    exitApp = exitApp,
                    openUri = { openUri(it) },
                )
            }
        }
    }
}
