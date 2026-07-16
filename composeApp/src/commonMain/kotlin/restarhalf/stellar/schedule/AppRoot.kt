package restarhalf.stellar.schedule

import androidx.compose.foundation.pager.rememberPagerState
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
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.update.ApkDownloadState
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.ui.components.screen.about.DownloadDialog
import restarhalf.stellar.schedule.ui.components.screen.about.UpdateConfirmDialog
import restarhalf.stellar.schedule.ui.koin.koinViewModel
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.screens.exclusion.WelcomeScreen
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.utils.Platform
import top.yukonga.miuix.kmp.utils.platform

/**
 * 应用根组件，负责初始化应用状态和管理全局UI逻辑
 * 
 * 该组件是整个应用的入口点，主要职责包括：
 * - 初始化ViewModel和依赖注入
 * - 管理应用全局状态（AppState）
 * - 处理应用更新检查和下载
 * - 管理隐私协议确认流程
 * - 提供CompositionLocal供子组件访问状态
 * 
 * @param appIcon 应用图标，用于更新对话框展示
 * @param pictureSelectorHost 图片选择器宿主组件，用于处理图片选择回调
 * @param ensureNotificationPermission 通知权限请求回调
 * @param openUri 打开URI的回调函数
 * @param showMessage 显示消息的回调函数（如Toast）
 * @param canSaveAwardPicture 是否可以保存奖励图片
 * @param saveAwardPicture 保存奖励图片的挂起函数
 * @param exitApp 退出应用的回调函数
 */
@Composable
fun AppRoot(
    appIcon: ImageBitmap? = null,
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
    exitApp: () -> Unit = {},
) {
    val vm: AppViewModel = koinViewModel()
    val bgVm: BackgroundViewModel = koinViewModel()
    val appUpdate: AppUpdatePort = koinInject()
    val appInfo: AppInfoPort = koinInject()
    val settings: ObservableSettings = koinInject(named(SettingsKeys.PREFS_NAME))

    val appUiState by vm.uiState.collectAsState()
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
        runCatching { appUpdate.check(currentVersionName = appInfo.versionName) }
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

    val apkDownloadState by appUpdate.apkDownloadState.collectAsState()
    var backgroundDownload by remember { mutableStateOf(false) }

    LaunchedEffect(apkDownloadState) {
        if (platform() != Platform.Android) return@LaunchedEffect
        when (val state = apkDownloadState) {
            is ApkDownloadState.Downloading -> {
                if (backgroundDownload) return@LaunchedEffect
                updateAppState { current -> current.copy(showApkDownloadDialog = true) }
            }

            is ApkDownloadState.Completed -> {
                backgroundDownload = false
                if (!appUpdate.canRequestInstallPackages()) {
                    showMessage("请允许安装未知应用后重试安装")
                    appUpdate.openUnknownSourcesSettings()
                } else {
                    val launched =
                        runCatching { appUpdate.launchInstaller(state.filePath) }
                            .onFailure { AppLogger.log("Update", "启动安装器失败", it) }
                            .getOrDefault(false)
                    if (!launched) {
                        showMessage("无法拉起安装，请在文件管理器中手动安装")
                    }
                }
                updateAppState { current -> current.copy(showApkDownloadDialog = false) }
            }

            is ApkDownloadState.Error -> {
                backgroundDownload = false
                showMessage(state.message)
                updateAppState { current -> current.copy(showApkDownloadDialog = false) }
            }

            ApkDownloadState.Idle -> {
                backgroundDownload = false
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
            appIcon = appIcon,
            pictureSelectorHost = pictureSelectorHost,
            pdfFilePickerHost = pdfFilePickerHost,
            ensureNotificationPermission = ensureNotificationPermission,
            openUri = openUri,
            showMessage = showMessage,
            canSaveAwardPicture = canSaveAwardPicture,
            saveAwardPicture = saveAwardPicture,
            saveLog = saveLog,
            saveCsv = saveCsv,
            runSync = runSync,
        )

        if (appState.showUpdateDialog) {
            UpdateConfirmDialog(
                show = showUpdateDialogState.value,
                onDismissRequest = { showUpdateDialogState.value = false },
                pendingUpdate = appState.pendingUpdate,
                onStartDownload = { info ->
                    if (platform() != Platform.Android) {
                        val openedDownload =
                            runCatching { appUpdate.launchInstaller(info.downloadUrl) }
                                .onFailure { AppLogger.log("Update", "打开下载链接失败", it) }
                                .getOrDefault(false)
                        val openedReleasePage =
                            if (openedDownload) true
                            else runCatching { appUpdate.launchInstaller(info.releasePageUrl) }
                                .onFailure { AppLogger.log("Update", "打开发布页失败", it) }
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
                show = showApkDownloadDialogState.value,
                onDismissRequest = { showApkDownloadDialogState.value = false },
                downloadProgress = progress,
                onStop = {
                    appUpdate.cancelApkDownload()
                    updateAppState { current -> current.copy(showApkDownloadDialog = false) }
                },
                onBackGround = {
                    backgroundDownload = true
                    updateAppState { current -> current.copy(showApkDownloadDialog = false) }
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
