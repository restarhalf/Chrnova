package restarhalf.stellar.schedule

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.russhwolf.settings.ObservableSettings
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.stats.DauReporter
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

    val appUiState by vm.uiState.collectAsStateWithLifecycle()
    var appState by remember {
        mutableStateOf(
            AppState(
                campus = appUiState.campus,
                termStartMs = appUiState.termStartMs,
                totalWeeks = appUiState.totalWeeks,
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
        val listener = settings.addIntListener(SettingsKeys.FLOATING_BAR, appState.barMode) { newValue ->
            updateAppState { current -> current.copy(barMode = newValue) }
        }
        onDispose { listener.deactivate() }
    }

    LaunchedEffect(appUiState) {
        updateAppState { current ->
            current.copy(
                campus = appUiState.campus,
                termStartMs = appUiState.termStartMs,
                totalWeeks = appUiState.totalWeeks,
            )
        }
    }

    val runSync = remember(vm, updateAppState) {
        suspend {
            vm.runSync { state ->
                updateAppState { current -> current.copy(syncUiState = state) }
            }
        }
    }

    LaunchedEffect(Unit) {
        val hasShown = settings.getBoolean(SettingsKeys.CONFIRM_PRIVACY, false)
        if (!hasShown) {
            updateAppState { current -> current.copy(confirmPrivacy = true) }
        }
    }

    LaunchedEffect(Unit) {
        // 经用例层检查，自动携带灰度 uid（学号哈希），与关于页手动检查行为一致
        runCatching { checkAppUpdate(currentVersionName = appInfo.versionName) }
            .onSuccess { latest ->
                if (latest != null) {
                    updateAppState { current ->
                        current.copy(
                            pendingUpdate = latest,
                            showUpdateDialog = true,
                        )
                    }
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

    val showUpdateDialogState =
        linkedMutableState(
            valueProvider = { appState.showUpdateDialog },
            onValueChange = { visible ->
                updateAppState { current -> current.copy(showUpdateDialog = visible) }
            },
        )
    val confirmPrivacyState =
        linkedMutableState(
            valueProvider = { appState.confirmPrivacy },
            onValueChange = { visible ->
                updateAppState { current -> current.copy(confirmPrivacy = visible) }
            },
        )

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

        if (appState.showUpdateDialog) {
            UpdateConfirmDialog(
                show = showUpdateDialogState.value,
                onDismissRequest = { showUpdateDialogState.value = false },
                pendingUpdate = appState.pendingUpdate,
                onStartDownload = { info ->
                    appUpdate.startDirectDownload(info)
                },
            )
        }

        if (appState.confirmPrivacy) {
            val welcomePagerState = rememberPagerState(pageCount = { 6 })
            WelcomeScreen(
                show = confirmPrivacyState,
                pagerState = welcomePagerState,
                exitApp = exitApp,
                openUri = { openUri(it) },
            )
        }
    }
}

private fun linkedMutableState(
    valueProvider: () -> Boolean,
    onValueChange: (Boolean) -> Unit,
): MutableState<Boolean> =
    object : MutableState<Boolean> {
        override var value: Boolean
            get() = valueProvider()
            set(value) {
                onValueChange(value)
            }

        override operator fun component1(): Boolean = value

        override operator fun component2(): (Boolean) -> Unit = { updated ->
            value = updated
        }
    }
