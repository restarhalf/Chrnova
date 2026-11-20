package restarhalf.stellar.schedule

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.haooz.softglass.GlassEnvironment
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.backdrops.LayerBackdrop
import com.haooz.softglass.backdrop.backdrops.layerBackdrop as glassLayerBackdrop
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.platform.isRenderEffectSupported
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.domain.usecase.SyncExactAlarmPermissionUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher
import restarhalf.stellar.schedule.ui.components.LocalComponentsAlpha
import restarhalf.stellar.schedule.ui.components.LocalGlassRedrawSignal
import restarhalf.stellar.schedule.ui.components.LocalSoftGlassSurface
import restarhalf.stellar.schedule.ui.components.LocalWallpaperBackdrop
import restarhalf.stellar.schedule.ui.components.WallpaperLayer
import restarhalf.stellar.schedule.ui.components.bottombar.rememberLiquidBottomBarFocusState
import restarhalf.stellar.schedule.ui.components.makeGlassOriginTransform
import restarhalf.stellar.schedule.ui.components.rememberScheduleGlassStyle
import restarhalf.stellar.schedule.ui.components.rememberScheduleGlassSurface
import restarhalf.stellar.schedule.ui.model.home.resolveComponentsAlpha
import restarhalf.stellar.schedule.ui.navigation.AppBottomBar
import restarhalf.stellar.schedule.ui.navigation.AppChromeState
import restarhalf.stellar.schedule.ui.navigation.AppNavigator
import restarhalf.stellar.schedule.ui.navigation.AppScaffoldBody
import restarhalf.stellar.schedule.ui.navigation.LocalAppChromeState
import restarhalf.stellar.schedule.ui.navigation.LocalMainPagerState
import restarhalf.stellar.schedule.ui.navigation.LocalNavigator
import restarhalf.stellar.schedule.ui.navigation.RootTabs
import restarhalf.stellar.schedule.ui.navigation.Screen
import restarhalf.stellar.schedule.ui.navigation.TransparentStackTransition
import restarhalf.stellar.schedule.ui.navigation.graph.AppNavDeps
import restarhalf.stellar.schedule.ui.navigation.graph.announcementEntries
import restarhalf.stellar.schedule.ui.navigation.graph.courseSelectionEntries
import restarhalf.stellar.schedule.ui.navigation.graph.evaluationEntries
import restarhalf.stellar.schedule.ui.navigation.graph.miscEntries
import restarhalf.stellar.schedule.ui.navigation.graph.papersEntries
import restarhalf.stellar.schedule.ui.navigation.graph.peEntries
import restarhalf.stellar.schedule.ui.navigation.rememberMainPagerState
import restarhalf.stellar.schedule.ui.screens.CustomizeDraft
import restarhalf.stellar.schedule.ui.screens.CustomizeOverlay
import restarhalf.stellar.schedule.ui.screens.CustomizePageEasing
import restarhalf.stellar.schedule.ui.screens.main.MainRouteContent
import restarhalf.stellar.schedule.ui.screens.main.MainScreenBackHandler
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import restarhalf.stellar.schedule.ui.viewmodel.AppViewModel
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import restarhalf.stellar.schedule.ui.viewmodel.SettingsViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppContent(
    vm: AppViewModel,
    bgVm: BackgroundViewModel,
    appUpdate: AppUpdatePort,
    wallpaperBackdrop: LayerBackdrop,
    foregroundBackdrop: LayerBackdrop,
    combinedBackdrop: GlassBackdrop,
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
    runSync: suspend () -> Unit,
    saveCsv: suspend (fileName: String, content: String) -> String? = { _, _ -> null },
    canSaveImage: Boolean = false,
    saveImage: suspend (fileName: String, bytes: ByteArray) -> Boolean = { _, _ -> false },
    onRequestWidgetRefresh: () -> Unit = {},
) {
    val appState = LocalAppState.current
    val appUiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    val backgroundUiState by bgVm.uiState.collectAsStateWithLifecycle()
    val settingsVm: SettingsViewModel = koinViewModel()
    val settingsUiState by settingsVm.uiState.collectAsStateWithLifecycle()
    val surfaceColor = colors.surface

    val pagerState = rememberPagerState(pageCount = { RootTabs.size })
    val mainPagerState = rememberMainPagerState(pagerState)
    LaunchedEffect(mainPagerState.pagerState.currentPage) {
        mainPagerState.syncPage()
    }

    val backStack = rememberNavBackStack<Screen>(Screen.Main)
    val navigator = remember(backStack) { AppNavigator(backStack) }
    val currentRoute = (navigator.current() as? Screen) ?: Screen.Main
    val chromeState =
        remember(currentRoute, mainPagerState.selectedPage) {
            AppChromeState(
                currentScreen = mainPagerState.currentScreen,
                isMainRoute = currentRoute == Screen.Main,
            )
        }
    val bottomBarFocusState = rememberLiquidBottomBarFocusState()
    var wasMainRoute by remember { mutableStateOf(chromeState.isMainRoute) }
    if (chromeState.isMainRoute && !wasMainRoute) {
        bottomBarFocusState.expand()
    }
    wasMainRoute = chromeState.isMainRoute
    LaunchedEffect(mainPagerState.selectedPage) {
        if (chromeState.isMainRoute) {
            bottomBarFocusState.expand()
        }
    }
    val pictureSelectorHostState by rememberUpdatedState(pictureSelectorHost)
    val ensureNotificationPermissionState by rememberUpdatedState(ensureNotificationPermission)
    val ensureExactAlarmPermissionState by rememberUpdatedState(ensureExactAlarmPermission)
    val onRequestDndPolicyPermissionState by rememberUpdatedState(onRequestDndPolicyPermission)
    val openUriState by rememberUpdatedState(openUri)
    val showMessageState by rememberUpdatedState(showMessage)
    val saveAwardPictureState by rememberUpdatedState(saveAwardPicture)
    val saveImageState by rememberUpdatedState(saveImage)
    val canSaveImageState by rememberUpdatedState(canSaveImage)
    val onRequestWidgetRefreshState by rememberUpdatedState(onRequestWidgetRefresh)
    val runSyncState by rememberUpdatedState(runSync)
    val announcementHttpClient = koinInject<HttpClient>(named("announcement"))
    val saveImageFromUrl: suspend (String) -> Boolean = remember(
        announcementHttpClient,
        saveImageState,
    ) {
        { url ->
            withContext(AppIoDispatcher) {
                runCatching {
                    val bytes = announcementHttpClient.get(url).body<ByteArray>()
                    saveImageState(deriveImageFileName(url), bytes)
                }.getOrDefault(false)
            }
        }
    }
    val announcementVm: AnnouncementViewModel = koinViewModel()
    val syncExactAlarmPermission: SyncExactAlarmPermissionUseCase = koinInject()
    val exactAlarmScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                announcementVm.refresh()
                exactAlarmScope.launch {
                    withContext(AppIoDispatcher) {
                        runCatching { syncExactAlarmPermission() }
                            .onFailure {
                                if (it is CancellationException) throw it
                            }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navCornerRadius = rememberNavSystemCornerRadius()
    val navEffects =
        remember(navCornerRadius) {
            NavDisplayEffects(
                enableCornerClip = true,
                cornerClipRadius = navCornerRadius,
                cornerClipMode = NavCornerClipMode.Leading,
                dimAmount = 0f,
                blockInputDuringTransition = true,
            )
        }

    val deps = AppNavDeps(
        navigator = navigator,
        appUpdate = appUpdate,
        bgVm = bgVm,
        announcementVm = announcementVm,
        appVm = vm,
        runSync = runSync,
        ensureExactAlarmPermission = ensureExactAlarmPermissionState,
        onRequestDndPolicyPermission = onRequestDndPolicyPermissionState,
        saveCsv = saveCsv,
        totalWeeks = appUiState.totalWeeks,
        pictureSelectorHost = pictureSelectorHostState,
        pdfFilePickerHost = pdfFilePickerHost,
        ensureNotificationPermission = ensureNotificationPermissionState,
        openUri = openUriState,
        showMessage = showMessageState,
        canSaveAwardPicture = canSaveAwardPicture,
        saveAwardPicture = saveAwardPictureState,
        canSaveImage = canSaveImageState,
        saveImageFromUrl = saveImageFromUrl,
        onLogoutJwxt = { vm.logout() },
        onRequestWidgetRefresh = onRequestWidgetRefreshState,
    )

    val customizeScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var customizeVisible by remember { mutableStateOf(false) }
    var customizeExiting by remember { mutableStateOf(false) }
    var customizeSheetOpen by remember { mutableStateOf(false) }
    var customizeDraft by remember { mutableStateOf<CustomizeDraft?>(null) }
    val customizeScale = remember { Animatable(1f) }
    val sheetShift = remember { Animatable(0f) }
    val sheetShiftTargetPx = with(density) { 90.dp.toPx() }

    fun openCustomize() {
        if (customizeVisible) return
        customizeDraft =
            CustomizeDraft(
                brightness = backgroundUiState.backgroundBrightness,
                blur = backgroundUiState.backgroundBlur,
                backgroundAlpha = backgroundUiState.backgroundAlpha,
                componentsAlpha = backgroundUiState.componentsAlpha,
                wallpaperOffset = backgroundUiState.wallpaperOffset,
                wallpaperScale = backgroundUiState.wallpaperScale,
                cardBlurPx = backgroundUiState.cardBlurPx,
                cardRefraction = backgroundUiState.cardRefraction,
                cardEdgeThickness = backgroundUiState.cardEdgeThickness,
                cardRim = backgroundUiState.cardRim,
            )
        customizeExiting = false
        customizeSheetOpen = false
        customizeVisible = true
        customizeScope.launch {
            customizeScale.animateTo(0.75f, tween(380, easing = CustomizePageEasing))
        }
    }

    fun closeCustomize(apply: Boolean) {
        if (!customizeVisible || customizeExiting) return
        customizeExiting = true
        customizeSheetOpen = false
        val draft = customizeDraft
        if (apply && draft != null) {
            bgVm.onBackgroundBrightnessChanged(draft.brightness)
            bgVm.onBackgroundBlurChanged(draft.blur)
            bgVm.onBackgroundAlphaChanged(draft.backgroundAlpha)
            bgVm.onComponentsAlphaChanged(draft.componentsAlpha)
            bgVm.onCardBlurPxChanged(draft.cardBlurPx)
            bgVm.onCardRefractionChanged(draft.cardRefraction)
            bgVm.onCardEdgeThicknessChanged(draft.cardEdgeThickness)
            bgVm.onCardRimChanged(draft.cardRim)
            if (draft.wallpaperOffset != backgroundUiState.wallpaperOffset ||
                draft.wallpaperScale != backgroundUiState.wallpaperScale
            ) {
                bgVm.onWallpaperTransformChanged(draft.wallpaperOffset, draft.wallpaperScale)
            }
        }
        customizeScope.launch {
            customizeScale.animateTo(1f, tween(480, easing = CustomizePageEasing))
            customizeDraft = null
            customizeVisible = false
            customizeExiting = false
        }
    }

    LaunchedEffect(customizeSheetOpen, customizeVisible) {
        val target = if (customizeVisible && customizeSheetOpen) -sheetShiftTargetPx else 0f
        sheetShift.animateTo(target, tween(400, easing = CustomizePageEasing))
    }

    val customizeBackState = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = customizeBackState,
        isBackEnabled = customizeVisible && !customizeExiting,
        onBackCompleted = { closeCustomize(apply = false) },
    )

    val hasWallpaper = backgroundUiState.wallpaper != null
    val effectiveComponentsAlpha =
        resolveComponentsAlpha(
            hasWallpaper = hasWallpaper,
            componentsAlpha = customizeDraft?.componentsAlpha ?: backgroundUiState.componentsAlpha,
        )

    // GlassEnvironment 的 hasWallpaper 只在这里知道：按压高光按「有壁纸 / 无壁纸」分两档亮度。
    SideEffect { GlassEnvironment.update(hasWallpaper = hasWallpaper) }

    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalAppChromeState provides chromeState,
        LocalMainPagerState provides mainPagerState,
        // 壁纸层必须在根部可见：BlurBottomSheet / OverlayDialog 实际渲染在根 Scaffold 的
        // MiuixPopupHost 里，处于 AppScaffoldBody 作用域之外；不在这里供给，弹窗读到的
        // LocalWallpaperBackdrop 就是 null，玻璃（sheetBackdrop、AppCard 卡片）会退回实色。
        LocalWallpaperBackdrop provides
            if (hasWallpaper) wallpaperBackdrop else null,
        // 同理：弹窗里的 AppCard 玻璃分支要求组件不透明度 < 1，默认值 1f 会让它走实色 Card
        LocalComponentsAlpha provides effectiveComponentsAlpha,
    ) {
        if (!customizeVisible) {
            MainScreenBackHandler(mainPagerState, navigator)
        }
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.surface),
        ) {
            // ── 柔光玻璃采样源：整页只建一次（一座壁纸金字塔 + 一次全尺寸像素处理）──
            val screenWidthPx = with(density) { maxWidth.toPx() }
            val screenHeightPx = with(density) { maxHeight.toPx() }
            val effectiveWallpaperAlpha =
                customizeDraft?.backgroundAlpha ?: backgroundUiState.backgroundAlpha
            val effectiveWallpaperBlur =
                customizeDraft?.blur ?: backgroundUiState.backgroundBlur
            val effectiveWallpaperBrightness =
                customizeDraft?.brightness ?: backgroundUiState.backgroundBrightness
            val effectiveWallpaperOffset =
                customizeDraft?.wallpaperOffset ?: backgroundUiState.wallpaperOffset
            val effectiveWallpaperScale =
                customizeDraft?.wallpaperScale ?: backgroundUiState.wallpaperScale
            val effectiveCardBlurPx =
                customizeDraft?.cardBlurPx ?: backgroundUiState.cardBlurPx
            val effectiveCardRefraction =
                customizeDraft?.cardRefraction ?: backgroundUiState.cardRefraction
            val effectiveCardEdgeThickness =
                customizeDraft?.cardEdgeThickness ?: backgroundUiState.cardEdgeThickness
            val effectiveCardRim =
                customizeDraft?.cardRim ?: backgroundUiState.cardRim
            val cardGlassStyle = rememberScheduleGlassStyle(
                cardBlurRadiusPx = effectiveCardBlurPx,
                cardRefraction = effectiveCardRefraction,
                cardEdgeThickness = effectiveCardEdgeThickness,
                cardRim = effectiveCardRim,
            )
            val glassOriginTransform: (Offset) -> Offset = remember(
                screenWidthPx, screenHeightPx, customizeScale, sheetShift,
            ) {
                makeGlassOriginTransform(
                    scale = { customizeScale.value },
                    translationY = { sheetShift.value },
                    pivotX = screenWidthPx * 0.5f,
                    pivotY = screenHeightPx * 0.5f,
                )
            }
            val softGlassSurface = rememberScheduleGlassSurface(
                wallpaperBitmap = backgroundUiState.wallpaper,
                wallpaperScale = effectiveWallpaperScale,
                wallpaperOffset = effectiveWallpaperOffset,
                screenWidthPx = screenWidthPx,
                screenHeightPx = screenHeightPx,
                wallpaperBrightness = effectiveWallpaperBrightness,
                wallpaperAlpha = effectiveWallpaperAlpha,
                baseColor = surfaceColor,
                style = cardGlassStyle,
                originTransform = glassOriginTransform,
            )
            // 首页大卡在主 pager 里：切 tab 时整层被 placeWithLayer 平移，子树不重绘，
            // 玻璃必须自己订阅这些位置量才能逐帧跟手。
            val glassRedraw: () -> Unit = remember(mainPagerState, customizeScale, sheetShift) {
                {
                    mainPagerState.pagerState.currentPageOffsetFraction
                    customizeScale.value
                    sheetShift.value
                }
            }
            val cardOffsetYPx: () -> Float =
                remember(screenHeightPx, customizeScale, sheetShift) {
                    {
                        val scaleProgress = ((customizeScale.value - 0.65f) / 0.35f).coerceIn(0f, 1f)
                        (screenHeightPx * 0.028f + sheetShift.value) * (1f - scaleProgress)
                    }
                }
            val customizeScaleProvider: () -> Float =
                remember(customizeScale) { { customizeScale.value } }

            val mainLayerNeeded by remember {
                derivedStateOf {
                    customizeVisible || customizeScale.value != 1f || sheetShift.value != 0f
                }
            }
            val glassDirtyUntil = remember { arrayOfNulls<TimeMark>(1) }
            val markGlassDirty: (Long) -> Unit = remember(glassDirtyUntil) {
                { durationMs ->
                    glassDirtyUntil[0] = TimeSource.Monotonic.markNow() + durationMs.milliseconds
                }
            }
            LaunchedEffect(currentRoute, mainPagerState.selectedPage) {
                markGlassDirty(550L)
            }
            val glassScrollConnection = remember(markGlassDirty) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        if (available != Offset.Zero) markGlassDirty(450L)
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource,
                    ): Offset {
                        if (consumed != Offset.Zero || available != Offset.Zero) markGlassDirty(450L)
                        return Offset.Zero
                    }

                    override suspend fun onPreFling(available: Velocity): Velocity {
                        if (available != Velocity.Zero) markGlassDirty(650L)
                        return Velocity.Zero
                    }

                    override suspend fun onPostFling(
                        consumed: Velocity,
                        available: Velocity,
                    ): Velocity {
                        markGlassDirty(650L)
                        return Velocity.Zero
                    }
                }
            }
            val wallpaperRecordKey = remember(
                hasWallpaper,
                backgroundUiState.wallpaper,
                effectiveWallpaperAlpha,
                effectiveWallpaperBlur,
                effectiveWallpaperBrightness,
                effectiveWallpaperOffset,
                effectiveWallpaperScale,
                surfaceColor,
            ) {
                listOf(
                    hasWallpaper,
                    backgroundUiState.wallpaper,
                    effectiveWallpaperAlpha,
                    effectiveWallpaperBlur,
                    effectiveWallpaperBrightness,
                    effectiveWallpaperOffset,
                    effectiveWallpaperScale,
                    surfaceColor,
                )
            }
            val glassRecordKey = remember(
                currentRoute,
                mainPagerState.selectedPage,
                effectiveComponentsAlpha,
                hasWallpaper,
                effectiveCardBlurPx,
                effectiveCardRefraction,
                effectiveCardEdgeThickness,
                effectiveCardRim,
                softGlassSurface,
                surfaceColor,
                customizeVisible,
                appState.themeMode,
                settingsUiState.showNonCurrentWeek,
                settingsUiState.scheduleRowHeight,
            ) {
                listOf(
                    currentRoute,
                    mainPagerState.selectedPage,
                    effectiveComponentsAlpha,
                    hasWallpaper,
                    effectiveCardBlurPx,
                    effectiveCardRefraction,
                    effectiveCardEdgeThickness,
                    effectiveCardRim,
                    softGlassSurface,
                    surfaceColor,
                    customizeVisible,
                    appState.themeMode,
                    settingsUiState.showNonCurrentWeek,
                    settingsUiState.scheduleRowHeight,
                )
            }
            val glassMustRecord: () -> Boolean = remember(pagerState, customizeScale, sheetShift, glassDirtyUntil) {
                var lastCustomizeScale = Float.NaN
                var lastSheetShift = Float.NaN
                {
                    val scale = customizeScale.value
                    val shift = sheetShift.value
                    val transformMoved = scale != lastCustomizeScale || shift != lastSheetShift
                    lastCustomizeScale = scale
                    lastSheetShift = shift
                    pagerState.isScrollInProgress ||
                        abs(pagerState.currentPageOffsetFraction) > 0.001f ||
                        customizeScale.isRunning ||
                        sheetShift.isRunning ||
                        transformMoved ||
                        (glassDirtyUntil[0]?.hasPassedNow() == false)
                }
            }

            CompositionLocalProvider(
                LocalSoftGlassSurface provides softGlassSurface,
                LocalGlassRedrawSignal provides glassRedraw,
            ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (mainLayerNeeded) {
                                Modifier
                                    .offset { IntOffset(0, cardOffsetYPx().roundToInt()) }
                                    .graphicsLayer {
                                        scaleX = customizeScale.value
                                        scaleY = customizeScale.value
                                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    }
                            } else {
                                Modifier
                            },
                        ),
            ) {
                Scaffold(
                    containerColor = colors.background.copy(alpha = 0f),
                    bottomBar = {
                        AppBottomBar(
                            glassBackdrop = combinedBackdrop,
                            focusState = bottomBarFocusState,
                        )
                    },
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // 背景 + 壁纸独立采样层：仅壁纸或主题底色变化时重录，滚屏/翻页/点击零重录壁纸
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (AppMaterialSettings.glassAvailable()) {
                                            Modifier.glassLayerBackdrop(
                                                backdrop = wallpaperBackdrop,
                                                recordKey = wallpaperRecordKey,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .background(surfaceColor),
                        ) {
                            backgroundUiState.wallpaper?.let { wallpaper ->
                                WallpaperLayer(
                                    image = wallpaper,
                                    alpha = effectiveWallpaperAlpha,
                                    blur = effectiveWallpaperBlur,
                                    brightness = effectiveWallpaperBrightness,
                                    offset = effectiveWallpaperOffset,
                                    scale = effectiveWallpaperScale,
                                    backdrop = null,
                                    baseColor = Color.Transparent,
                                )
                            }
                        }

                        // 前景采样层：只录前景路由内容，不兜壁纸也不对普通点按盲目续期录制
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .nestedScroll(glassScrollConnection)
                                    .nestedScroll(bottomBarFocusState.nestedScrollConnection)
                                    .glassLayerBackdrop(
                                        backdrop = foregroundBackdrop,
                                        recordKey = glassRecordKey,
                                        mustRecord = glassMustRecord,
                                    ),
                        ) {
                            AppScaffoldBody(
                                innerPadding = innerPadding,
                                componentsAlpha = effectiveComponentsAlpha,
                                wallpaperBackdrop = if (hasWallpaper) wallpaperBackdrop else null,
                                liquidGlassBackdrop = combinedBackdrop,
                            ) {
                                NavDisplay(
                                    backStack = backStack,
                                    onBack = { navigator.pop() },
                                    transition = TransparentStackTransition,
                                    effects = navEffects,
                                ) {
                                    entry<Screen.Main> {
                                        MainRouteContent(
                                            bgVm = bgVm,
                                            vm = vm,
                                            announcementVm = announcementVm,
                                            runSync = runSyncState,
                                            onChangeBackground = { openCustomize() },
                                            showMessage = showMessage,
                                            canSaveImage = canSaveImageState,
                                            saveImage = saveImageState,
                                        )
                                    }
                                    peEntries(deps)
                                    papersEntries(deps)
                                    evaluationEntries(deps)
                                    courseSelectionEntries(deps)
                                    announcementEntries(deps)
                                    miscEntries(deps)
                                }
                            }
                        }
                    }
                }
                val warmupBlurPx = with(density) { AppMaterialSettings.glassBlurDp().dp.toPx() }
                BlurWarmup(
                    enabled = AppMaterialSettings.glassEffectsEnabled(),
                    backdrop = combinedBackdrop,
                    blurPx = warmupBlurPx,
                )
            }

            if (customizeVisible) {
                CustomizeOverlay(
                    vm = bgVm,
                    scaleProvider = customizeScaleProvider,
                    offsetYPxProvider = cardOffsetYPx,
                    selectedPage = mainPagerState.selectedPage,
                    onSelectPage = { mainPagerState.animateToPage(it) },
                    onSheetOpenChange = { customizeSheetOpen = it },
                    onDraftChange = { customizeDraft = it },
                    exiting = customizeExiting,
                    onRequestClose = { apply -> closeCustomize(apply) },
                    pictureSelectorHost = pictureSelectorHostState,
                    themeMode = appState.themeMode,
                    onThemeModeChange = { settingsVm.onThemeModeChanged(it) },
                    showNonCurrentWeek = settingsUiState.showNonCurrentWeek,
                    onShowNonCurrentWeekChange = { settingsVm.onShowNonCurrentWeekChanged(it) },
                    rowHeightDp = settingsUiState.scheduleRowHeight,
                    rowHeightRange = DisplaySettingsPort.MIN_ROW_HEIGHT_DP..DisplaySettingsPort.MAX_ROW_HEIGHT_DP,
                    onRowHeightChange = { settingsVm.onScheduleRowHeightChanged(it) },
                )
            }
            }
        }
    }
}

private fun deriveImageFileName(url: String): String {
    val tail = url.substringAfterLast('/', "")
        .substringBefore('?')
        .substringBefore('#')
        .takeIf { it.isNotBlank() && !it.contains('/') && it.length <= 120 }
        ?: return "chrnova_${Clock.System.now().toEpochMilliseconds()}.jpg"
    return if (tail.contains('.')) tail else "$tail.jpg"
}

/** 首帧后预热 RenderEffect 模糊管线，避免首次弹出毛玻璃弹窗时现编 shader 掉帧。 */
@Composable
private fun BlurWarmup(enabled: Boolean, backdrop: GlassBackdrop, blurPx: Float) {
    var ready by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { ready = 1 }
    if (ready == 0 || !enabled || !isRenderEffectSupported()) return
    Box(
        modifier = Modifier
            .size(1.dp)
            .graphicsLayer { alpha = 0f }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(36.dp) },
                effects = { blur(blurPx) },
            ),
    )
}
