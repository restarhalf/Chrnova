package restarhalf.stellar.schedule

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.russhwolf.settings.ObservableSettings
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.stats.DauReporter
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.CheckAppUpdateUseCase
import restarhalf.stellar.schedule.ui.components.screen.about.UpdateConfirmDialog
import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.screens.exclusion.WelcomeScreen
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 应用根组件，负责初始化应用状态和管理全局UI逻辑
 */
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
        onPicked: (ByteArray, String, String) -> Unit,
    ) -> Unit = {},
    ensureNotificationPermission: (onGranted: () -> Unit) -> Unit = { onGranted -> onGranted() },
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
    // Coil 网络 fetcher 需显式注册：Android 经 ServiceLoader 自动注册，
    // iOS/native 无该机制，不配置则单例 ImageLoader 无网络能力，所有网络图加载失败。
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
    val settings: ObservableSettings = koinInject(named(SettingsKeys.PREFS_NAME))
    val settingsPort: SettingsPort = koinInject()

    // 全局 UI 偏好（对齐 miuix example：AppState 只放界面开关，业务数据走 ViewModel）
    var appState by remember {
        mutableStateOf(
            AppState(
                themeMode = settings.getInt(SettingsKeys.THEME_MODE, 0),
                barMode = settings.getInt(SettingsKeys.FLOATING_BAR, 0),
            ),
        )
    }
    val updateAppState = remember {
        { transform: (AppState) -> AppState ->
            val updated = transform(appState)
            if (updated != appState) {
                appState = updated
            }
        }
    }

    DisposableEffect(settings) {
        val barListener = settings.addIntListener(SettingsKeys.FLOATING_BAR, appState.barMode) { newValue ->
            updateAppState { current -> current.copy(barMode = newValue) }
        }
        val themeListener = settings.addIntListener(SettingsKeys.THEME_MODE, appState.themeMode) { newValue ->
            updateAppState { current -> current.copy(themeMode = newValue) }
        }
        onDispose {
            barListener.deactivate()
            themeListener.deactivate()
        }
    }

    // 主题模式外推给宿主（Android 用于 edge-to-edge 系统栏），含初始值同步
    val currentOnThemeModeChange by rememberUpdatedState(onThemeModeChange)
    LaunchedEffect(Unit) {
        snapshotFlow { appState.themeMode }.collect { currentOnThemeModeChange(it) }
    }

    val runSync = remember(vm) {
        suspend { vm.runSync() }
    }

    // 更新弹窗与隐私引导属于根级一次性流程状态，仅 AppRoot 自用，无需下发
    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val showWelcome = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val hasShown = settings.getBoolean(SettingsKeys.CONFIRM_PRIVACY, false)
        if (!hasShown) {
            showWelcome.value = true
        }
    }

    LaunchedEffect(Unit) {
        // 经用例层检查，自动携带灰度 uid（学号哈希），与关于页手动检查行为一致
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
        // 每日匿名日活上报：仅发送本地随机设备标识，不含任何个人信息；失败静默不影响使用
        runCatching { DauReporter.pingTodayIfDue(settings = settings, deviceId = settingsPort.getDeviceId()) }
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
            LocalUpdateAppState provides updateAppState,
        ) {
            AppContent(
                vm = vm,
                bgVm = bgVm,
                appUpdate = appUpdate,
                pictureSelectorHost = pictureSelectorHost,
                pdfFilePickerHost = pdfFilePickerHost,
                ensureNotificationPermission = ensureNotificationPermission,
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
