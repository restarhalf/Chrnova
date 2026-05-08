package restarhalf.stellar.schedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.core.update.ApkDownloadState
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.ui.components.screen.about.DownloadDialog
import restarhalf.stellar.schedule.ui.components.screen.about.UpdateConfirmDialog
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.navigation.FirstOpenNoticeDialog
import restarhalf.stellar.schedule.ui.navigation.shouldShowSplitPane
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.utils.Platform
import top.yukonga.miuix.kmp.utils.platform

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

    val appUiState by vm.uiState.collectAsState()
    var appState by remember {
        mutableStateOf(
            AppState(
                campus = appUiState.campus,
                termStartMs = appUiState.termStartMs,
                totalWeeks = appUiState.totalWeeks,
                isWideScreen = isWideScreen,
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

    LaunchedEffect(isWideScreen) {
        updateAppState { current -> current.copy(isWideScreen = isWideScreen) }
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
        val hasShown = settings.getBoolean(SettingsKeys.HAS_SHOWN_FIRST_OPEN_DIALOG, false)
        if (!hasShown) {
            settings[SettingsKeys.HAS_SHOWN_FIRST_OPEN_DIALOG] = true
            updateAppState { current -> current.copy(showFirstOpenDialog = true) }
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
            updateAppState { current ->
                current.copy(
                    pendingUpdate = latest,
                    showUpdateDialog = true,
                )
            }
        }
    }

    val apkDownloadState by appUpdate.apkDownloadState.collectAsState()

    LaunchedEffect(apkDownloadState) {
        if (platform() != Platform.Android) return@LaunchedEffect
        when (val state = apkDownloadState) {
            is ApkDownloadState.Downloading -> {
                updateAppState { current -> current.copy(showApkDownloadDialog = true) }
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
                updateAppState { current -> current.copy(showApkDownloadDialog = false) }
            }

            is ApkDownloadState.Error -> {
                showMessage(state.message)
                updateAppState { current -> current.copy(showApkDownloadDialog = false) }
            }

            ApkDownloadState.Idle -> {
                updateAppState { current -> current.copy(showApkDownloadDialog = false) }
            }
        }
    }

    val showUpdateDialogState =
        linkedMutableState(
            valueProvider = { appState.showUpdateDialog },
            onValueChange = { visible ->
                updateAppState { current -> current.copy(showUpdateDialog = visible) }
            },
        )
    val showApkDownloadDialogState =
        linkedMutableState(
            valueProvider = { appState.showApkDownloadDialog },
            onValueChange = { visible ->
                updateAppState { current -> current.copy(showApkDownloadDialog = visible) }
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
            appIcon = appIcon,
            pictureSelectorHost = pictureSelectorHost,
            ensureNotificationPermission = ensureNotificationPermission,
            openUri = openUri,
            showMessage = showMessage,
            canSaveAwardPicture = canSaveAwardPicture,
            saveAwardPicture = saveAwardPicture,
            runSync = runSync,
        )

        if (appState.showUpdateDialog) {
            UpdateConfirmDialog(
                show = showUpdateDialogState,
                pendingUpdate = appState.pendingUpdate,
                onStartDownload = { info ->
                    if (platform() != Platform.Android) {
                        val openedDownload =
                            runCatching { appUpdate.launchInstaller(info.downloadUrl) }
                                .getOrDefault(false)
                        val openedReleasePage =
                            if (openedDownload) true
                            else runCatching { appUpdate.launchInstaller(info.releasePageUrl) }
                                .getOrDefault(false)
                        showMessage(
                            if (openedReleasePage) "已打开下载链接，请在浏览器完成安装"
                            else "无法打开下载链接，请稍后重试",
                        )
                        return@UpdateConfirmDialog
                    }
                    appUpdate.startDirectDownload(info)
                },
            )
        }

        if (platform() == Platform.Android && appState.showApkDownloadDialog) {
            val progress =
                (apkDownloadState as? ApkDownloadState.Downloading)?.progress
                    ?: if (apkDownloadState is ApkDownloadState.Completed) 100f else 0f

            DownloadDialog(
                show = showApkDownloadDialogState,
                downloadProgress = progress,
                onStop = {
                    appUpdate.cancelApkDownload()
                    updateAppState { current -> current.copy(showApkDownloadDialog = false) }
                },
                onBackGround = {
                    updateAppState { current -> current.copy(showApkDownloadDialog = false) }
                },
            )
        }

        if (appState.showFirstOpenDialog) {
            FirstOpenNoticeDialog(
                show = appState.showFirstOpenDialog,
                onDismiss = {
                    updateAppState { current -> current.copy(showFirstOpenDialog = false) }
                },
                onExit = {
                    settings[SettingsKeys.HAS_SHOWN_FIRST_OPEN_DIALOG] = false
                    updateAppState { current -> current.copy(showFirstOpenDialog = false) }
                    exitApp()
                },
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
