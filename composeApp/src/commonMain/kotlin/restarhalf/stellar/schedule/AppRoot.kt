package restarhalf.stellar.schedule

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.haooz.softglass.GlassEnvironment
import com.haooz.softglass.GlassPreset
import com.haooz.softglass.backdrop.backdrops.rememberCombinedBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberLayerBackdrop as rememberGlassLayerBackdrop
import com.haooz.softglass.glass.BarFill
import com.haooz.softglass.glass.BrightLargeTone
import com.haooz.softglass.glass.BrightTone
import com.haooz.softglass.glass.ThemeFill
import com.haooz.softglass.glass.applySoftGlass
import kotlinx.coroutines.flow.first
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import restarhalf.stellar.schedule.data.impl.DauReporter
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.usecase.CheckAppUpdateUseCase
import restarhalf.stellar.schedule.ui.chrome.LocalSystemBarsDarkOverride
import restarhalf.stellar.schedule.ui.components.EDGE_SAFE_SOFT_GLASS_LIVE_SHADER
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import restarhalf.stellar.schedule.ui.components.screen.about.UpdateConfirmDialog
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import top.yukonga.miuix.kmp.material.GlassSurfaceAlphaDrop
import top.yukonga.miuix.kmp.material.SoftGlassLayer
import top.yukonga.miuix.kmp.material.LocalGlassBlurDp
import top.yukonga.miuix.kmp.material.LocalGlassDialogBlurDp
import top.yukonga.miuix.kmp.material.LocalGlassPanelBlurDp
import top.yukonga.miuix.kmp.material.LocalGlassSurfaceAlphaDrop
import top.yukonga.miuix.kmp.material.LocalMaterialNoneEnabled
import top.yukonga.miuix.kmp.material.LocalSoftGlassLayer
import restarhalf.stellar.schedule.ui.screens.exclusion.ForceUpdateScreen
import restarhalf.stellar.schedule.ui.screens.exclusion.WelcomeScreen
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalAppSettingDark
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
    onRequestDndPolicyPermission: () -> Unit = {},
    openUri: (String) -> Boolean = { false },
    showMessage: (String) -> Unit = {},
    canSaveAwardPicture: Boolean = false,
    saveAwardPicture: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    saveCsv: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    canSaveImage: Boolean = false,
    saveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    exitApp: () -> Unit = {},
    onEffectiveDarkChange: (Boolean) -> Unit = {},
    onRequestWidgetRefresh: () -> Unit = {},
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
    val exactAlarmPermission: ExactAlarmPermissionPort = koinInject()

    LaunchedEffect(Unit) {
        val enabled =
            reminderSettings.observeCourseReminderEnabled().first() ||
                reminderSettings.observeExamReminderEnabled().first()
        if (enabled && !exactAlarmPermission.hasAsked()) {
            ensureExactAlarmPermission()
        }
    }

    var appState by remember {
        mutableStateOf(AppState(themeMode = displaySettings.getThemeMode()))
    }
    LaunchedEffect(displaySettings) {
        displaySettings.observeThemeMode().collect { themeMode ->
            val updated = appState.copy(themeMode = themeMode)
            if (updated != appState) {
                appState = updated
            }
        }
    }

    val bgUiState by bgVm.uiState.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val effectiveDark =
        when (appState.themeMode) {
            DisplaySettingsPort.THEME_MODE_LIGHT -> false
            DisplaySettingsPort.THEME_MODE_DARK -> true
            DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER -> bgUiState.wallpaperIsDark ?: systemDark
            else -> systemDark
        }
    val systemBarsDarkOverride = remember { mutableStateOf<Boolean?>(null) }
    val currentOnEffectiveDarkChange by rememberUpdatedState(onEffectiveDarkChange)
    LaunchedEffect(effectiveDark, systemBarsDarkOverride.value) {
        currentOnEffectiveDarkChange(systemBarsDarkOverride.value ?: effectiveDark)
    }

    val runSync = remember(vm) {
        suspend { vm.runSync() }
    }

    var pendingUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var forceUpdateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }
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
                    if (latest.isForceUpdate) {
                        forceUpdateInfo = latest
                    } else {
                        pendingUpdate = latest
                        showUpdateDialog = true
                    }
                }
            }
    }

    LaunchedEffect(Unit) {

        runCatching { DauReporter.pingTodayIfDue(deviceSettings = deviceSettings) }
    }

    val themeController =
        remember(appState.themeMode, effectiveDark) {
            ThemeController(
                colorSchemeMode =
                    when (appState.themeMode) {
                        DisplaySettingsPort.THEME_MODE_LIGHT -> ColorSchemeMode.Light
                        DisplaySettingsPort.THEME_MODE_DARK -> ColorSchemeMode.Dark
                        DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER ->
                            if (effectiveDark) ColorSchemeMode.Dark else ColorSchemeMode.Light
                        else -> ColorSchemeMode.System
                    },
            )
        }

    MiuixTheme(controller = themeController) {
        // 玻璃采样层在 AppContent 里挂载，但 UpdateConfirmDialog / CustomizeOverlay 等
        // 渲染在 AppScaffoldBody 之外，backdrop 提升到根部统一供给
        //
        // GlassEnvironment 是绘制期只读镜像（:miuix 的 SearchBar / SmallTitle /
        // InteractiveHighlight 读它）：这里是全应用唯一的 isDark 写入点，
        // 不写的话它们会一直读到默认的 false，浅色分支永远生效。
        val density = LocalDensity.current
        SideEffect { GlassEnvironment.update(isDark = effectiveDark, density = density.density) }
        // 不传自定义 onDraw（contentOnlyCapture = true），背景层（壁纸 + surface）与前景层各自独立录制，
        // 避免滚屏/翻页时陪着重录壁纸，也不重复重绘整屏。
        val wallpaperBackdrop = rememberGlassLayerBackdrop()
        val foregroundBackdrop = rememberGlassLayerBackdrop()
        val glassBackdrop = rememberCombinedBackdrop(wallpaperBackdrop, foregroundBackdrop)
        val softGlassLayer = remember(effectiveDark) {
            if (!AppMaterialSettings.glassEffectsEnabled()) {
                null
            } else {
                val small = AppMaterialSettings.chromeGlassStyle(
                    base = GlassPreset.Default.style,
                    isDark = effectiveDark,
                    fill = ThemeFill,
                    tone = BrightTone,
                )
                val large = AppMaterialSettings.chromeGlassStyle(
                    base = GlassPreset.Default.style,
                    isDark = effectiveDark,
                    fill = ThemeFill,
                    tone = BrightLargeTone,
                )
                val dialog = AppMaterialSettings.chromeGlassStyle(
                    base = GlassPreset.Default.style,
                    isDark = effectiveDark,
                    fill = BarFill,
                    tone = BrightLargeTone,
                )
                SoftGlassLayer(
                    shaderString = EDGE_SAFE_SOFT_GLASS_LIVE_SHADER,
                    applyUniforms = { width, height, radiusPx ->
                        applySoftGlass(small, width, height, radiusPx)
                    },
                    applyLargeUniforms = { width, height, radiusPx ->
                        applySoftGlass(large, width, height, radiusPx)
                    },
                    applyDialogUniforms = { width, height, radiusPx ->
                        applySoftGlass(dialog, width, height, radiusPx)
                    },
                )
            }
        }
        CompositionLocalProvider(
            LocalAppState provides appState,
            LocalSystemBarsDarkOverride provides systemBarsDarkOverride,
            LocalAppSettingDark provides effectiveDark,
            LocalLiquidGlassBackdrop provides glassBackdrop,
            LocalMaterialNoneEnabled provides !AppMaterialSettings.glassAvailable(),
            LocalGlassBlurDp provides AppMaterialSettings.glassBlurDp(),
            LocalGlassPanelBlurDp provides AppMaterialSettings.glassPanelBlurDp(),
            LocalGlassDialogBlurDp provides AppMaterialSettings.glassDialogBlurDp(),
            LocalGlassSurfaceAlphaDrop provides
                GlassSurfaceAlphaDrop(
                    light = AppMaterialSettings.glassSurfaceAlphaDrop(isLight = true),
                    dark = AppMaterialSettings.glassSurfaceAlphaDrop(isLight = false),
                ),
            LocalSoftGlassLayer provides softGlassLayer,
        ) {
            AppContent(
                vm = vm,
                bgVm = bgVm,
                appUpdate = appUpdate,
                wallpaperBackdrop = wallpaperBackdrop,
                foregroundBackdrop = foregroundBackdrop,
                combinedBackdrop = glassBackdrop,
                pictureSelectorHost = pictureSelectorHost,
                pdfFilePickerHost = pdfFilePickerHost,
                ensureNotificationPermission = ensureNotificationPermission,
                ensureExactAlarmPermission = ensureExactAlarmPermission,
                onRequestDndPolicyPermission = onRequestDndPolicyPermission,
                openUri = openUri,
                showMessage = showMessage,
                canSaveAwardPicture = canSaveAwardPicture,
                saveAwardPicture = saveAwardPicture,
                saveCsv = saveCsv,
                canSaveImage = canSaveImage,
                saveImage = saveImage,
                runSync = runSync,
                onRequestWidgetRefresh = onRequestWidgetRefresh,
                rootOverlayContent = {
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
                },
            )

            if (showWelcome.value) {
                val welcomePagerState = rememberPagerState(pageCount = { 6 })
                WelcomeScreen(
                    show = showWelcome,
                    pagerState = welcomePagerState,
                    exitApp = exitApp,
                    openUri = { openUri(it) },
                )
            }

            forceUpdateInfo?.let { info ->
                ForceUpdateScreen(
                    currentVersionName = appInfo.versionName,
                    updateInfo = info,
                    onStartDownload = { appUpdate.startDirectDownload(it) },
                    onDismiss = { forceUpdateInfo = null },
                )
            }
        }
    }
}
