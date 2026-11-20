package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haooz.softglass.shape.ContinuousCapsule
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.data.wallpaper.WIDGET_WALLPAPER_BLUR_DEFAULT
import restarhalf.stellar.schedule.data.wallpaper.WallpaperStore
import restarhalf.stellar.schedule.data.wallpaper.WidgetAppearance
import restarhalf.stellar.schedule.data.wallpaper.WidgetWallpaperCrop
import restarhalf.stellar.schedule.data.wallpaper.WidgetWallpaperStore
import restarhalf.stellar.schedule.data.wallpaper.updateWidgetCropForGesture
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.ui.chrome.LocalSystemBarsDarkOverride
import restarhalf.stellar.schedule.ui.components.LocalWallpaperBackdrop
import restarhalf.stellar.schedule.ui.components.SheetCloseButton
import restarhalf.stellar.schedule.ui.components.sheetContentBottomInset
import restarhalf.stellar.schedule.ui.icons.Close
import restarhalf.stellar.schedule.ui.icons.GridView
import restarhalf.stellar.schedule.ui.icons.Image
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.BlurBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.NavigationBackHandlerFor
import kotlin.math.roundToInt

/** 与课表外观页同一支底色 */
private val WidgetCustomizeScrim = Color(0xFF1A1A1A)

/** 底部工具钮底色（与课表外观页一致） */
private val WidgetCustomizeToolColor = Color(0xFF363636)

/** 已有壁纸时，壁纸钮变成「清除」（与课表外观页同一个红） */
private val WidgetCustomizeClearColor = Color(0xFFB71C1C)

private const val MOVE_IN_MILLIS = 460
private const val DIM_IN_MILLIS = 320
private const val TOOL_IN_MILLIS = 260
private const val MOVE_OUT_MILLIS = 360
private const val DIM_OUT_MILLIS = 260

/** 页面序号 → 小部件壁纸槽位。用「种类名」而不是序号当键：序号会随预览页增减漂移。 */
internal fun widgetWallpaperKey(page: Int): String = when (page) {
    0 -> WidgetWallpaperStore.KEY_TODAY
    1 -> WidgetWallpaperStore.KEY_TODAY_TOMORROW
    else -> WidgetWallpaperStore.KEY_SMALL
}

/** 面板里的小部件主题档位（顺序即展示顺序）。「跟随壁纸」只是选图那一刻的自动行为。 */
private val WidgetThemeOptions = listOf(
    DisplaySettingsPort.THEME_MODE_SYSTEM,
    DisplaySettingsPort.THEME_MODE_DARK,
    DisplaySettingsPort.THEME_MODE_LIGHT,
)

private fun widgetThemeLabel(mode: Int): String = when (mode) {
    DisplaySettingsPort.THEME_MODE_DARK -> "深色"
    DisplaySettingsPort.THEME_MODE_LIGHT -> "浅色"
    else -> "跟随系统"
}

/** 字号倍率可调区间 */
private val WidgetTextScaleRange = 0.8f..1.4f

/** 壁纸模糊可调区间（dp） */
private val WidgetWallpaperBlurRange = 0f..24f

/**
 * 小部件自定义浮层。
 *
 * 视觉与交互对齐课表外观页：深灰压屏、顶栏取消/应用、底部工具栏。
 * 差别只在「被编辑的主体」——课表页是小部件之外的卡片，这里就是小部件本身。
 *
 * 壁纸：选图 → 双指缩放/拖动取景 → 「应用」才落盘；「取消」一律不写。
 */
@Composable
fun WidgetCustomizeOverlay(
    request: WidgetCustomizeRequest,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    /**
     * 小部件已回到原位、浮层还挂着时回调一次。
     * 给宿主一个「先把页面里那份预览放出来」的机会。
     */
    onSettled: () -> Unit = {},
    /** 宿主拉起系统相册；回调带回图片 uri（取消传 null） */
    pictureSelectorHost: @Composable (
        show: Boolean,
        onDismissRequest: () -> Unit,
        onPicked: (String) -> Unit,
        outputWidthPx: Int?,
        outputHeightPx: Int?,
    ) -> Unit = { _, _, _, _, _ -> },
) {
    val densityObj = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // 没壁纸可跟随时的兜底主题
    val appIsDark = isAppDarkTheme()

    val wallpaperKey = remember(request.page) { widgetWallpaperKey(request.page) }
    // 「进浮层那一刻」的快照：取消要**立刻**还回这个样子
    var originalWallpaper by remember(wallpaperKey) {
        mutableStateOf(WidgetWallpaperStore.cached(wallpaperKey))
    }
    var originalCrop by remember(wallpaperKey) {
        mutableStateOf(WidgetWallpaperStore.loadCrop(wallpaperKey))
    }
    val storedThemeMode = remember(wallpaperKey) {
        WidgetWallpaperStore.loadThemeMode(wallpaperKey)
    }
    val originalThemeMode = remember(wallpaperKey) {
        resolveThemeMode(storedThemeMode, originalWallpaper, appIsDark)
    }
    val originalAppearance = remember(wallpaperKey) {
        WidgetWallpaperStore.loadAppearance(wallpaperKey)
    }
    // 首帧就得有图：页面刚加载过，缓存里已经有了，同步取一份。
    var wallpaper by remember(wallpaperKey) { mutableStateOf(originalWallpaper) }
    var wallpaperCrop by remember(wallpaperKey) { mutableStateOf(originalCrop) }
    var themeMode by remember(wallpaperKey) { mutableStateOf(originalThemeMode) }
    // 这一档是不是「自动定的」（旧档位折算 / 选图定深浅）。只有自动定的才在清图时退回跟随系统。
    var autoTheme by remember(wallpaperKey) {
        mutableStateOf(storedThemeMode == DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER)
    }
    var appearance by remember(wallpaperKey) { mutableStateOf(originalAppearance) }
    // 只在用户真的换了图/清了图时才写盘，避免每次「应用」都重编码一遍
    var wallpaperDirty by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    // 底部面板
    var showSheet by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val sheetShift = remember { Animatable(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(wallpaperKey) {
        // 只有缓存未命中（冷启动直进浮层）才需要读盘
        if (wallpaper != null || !WidgetWallpaperStore.exists(wallpaperKey)) return@LaunchedEffect
        val loaded = WidgetWallpaperStore.load(wallpaperKey, forceReload = true) ?: return@LaunchedEffect
        wallpaper = loaded
        wallpaperCrop = WidgetWallpaperStore.loadCrop(wallpaperKey)
        // 用户还没动过图，读回来的这份才是「原样」，取消时该还的是它
        if (!wallpaperDirty) {
            originalWallpaper = loaded
            originalCrop = wallpaperCrop
        }
    }

    val move = remember { Animatable(0f) }
    val scrim = remember { Animatable(0f) }
    val toolAlpha = remember { Animatable(0f) }
    val toolSlide = remember { Animatable(1f) }
    var closing by remember { mutableStateOf(false) }

    // 退出统一走这里：先把小部件送回原位再摘浮层，直接消失会断掉「从哪来回哪去」
    fun close(after: () -> Unit) {
        if (closing) return
        closing = true
        showSheet = false
        scope.launch {
            coroutineScope {
                launch { toolAlpha.animateTo(0f, tween(150)) }
                launch { scrim.animateTo(0f, tween(DIM_OUT_MILLIS)) }
                launch {
                    move.animateTo(
                        0f,
                        tween(MOVE_OUT_MILLIS, easing = CustomizePageEasing)
                    )
                }
            }
            onSettled()
            withFrameNanos { }
            after()
        }
    }

    /** 取消：**立刻**把预览还原成进浮层时的样子。 */
    fun revertPreview() {
        wallpaper = originalWallpaper
        wallpaperCrop = originalCrop
        themeMode = originalThemeMode
        autoTheme = storedThemeMode == DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER
        appearance = originalAppearance
        wallpaperDirty = false
    }

    fun cancel() {
        if (closing) return
        revertPreview()
        close(onDismiss)
    }

    // 「应用」：先把图写下去再退场 —— 浮层一摘，作用域里的协程就被取消了
    fun apply() {
        if (closing || saving) return
        saving = true
        scope.launch {
            val bitmap = wallpaper
            if (bitmap == null) {
                // 图被清掉了就删盘
                if (WidgetWallpaperStore.exists(wallpaperKey)) {
                    WidgetWallpaperStore.clear(wallpaperKey)
                }
            } else {
                // 只挪了取景也要存：图没换就别重编码一遍
                if (wallpaperDirty) WidgetWallpaperStore.save(wallpaperKey, bitmap)
                WidgetWallpaperStore.saveCrop(wallpaperKey, wallpaperCrop)
            }
            WidgetWallpaperStore.saveThemeMode(wallpaperKey, themeMode)
            WidgetWallpaperStore.saveAppearance(wallpaperKey, appearance)
            saving = false
            close(onApply)
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { move.animateTo(1f, tween(MOVE_IN_MILLIS, easing = CustomizePageEasing)) }
            // 压屏比位移晚一点起步：先让「这块是小部件」读出来，再把它周围压暗
            launch { scrim.animateTo(1f, tween(DIM_IN_MILLIS, easing = CustomizePageEasing)) }
            launch {
                coroutineScope {
                    launch { toolAlpha.animateTo(1f, tween(TOOL_IN_MILLIS, easing = CustomizePageEasing)) }
                    launch { toolSlide.animateTo(0f, tween(TOOL_IN_MILLIS, easing = CustomizePageEasing)) }
                }
            }
        }
    }

    NavigationBackHandlerFor(
        enabled = true,
        onBackCompleted = {
            if (showSheet) showSheet = false else cancel()
        },
        onBackCancelled = {},
    )

    val systemBarsDarkOverride = LocalSystemBarsDarkOverride.current
    DisposableEffect(Unit) {
        systemBarsDarkOverride.value = true
        onDispose { systemBarsDarkOverride.value = null }
    }

    val source = request.bounds
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    // 源矩形换到本层坐标
    val sourceRect = remember(source, overlayOrigin) {
        source.translate(-overlayOrigin.x, -overlayOrigin.y)
    }
    val primaryColor = MiuixTheme.colorScheme.primary

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin = it.positionInRoot() }
    ) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()

        val sourceW = if (sourceRect.width > 0f) sourceRect.width else screenW
        val sourceH = if (sourceRect.height > 0f) sourceRect.height else screenW

        // 只位移不缩放：落位后仍是原尺寸，用户改的是这张小部件本身
        val targetLeft = (screenW - sourceW) / 2f
        val targetTop = (screenH - sourceH) / 2f

        // 压屏：draw 阶段读 alpha，避免每帧重组整层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(WidgetCustomizeScrim.copy(alpha = scrim.value))
                }
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = { if (showSheet) showSheet = false }
                )
        )

        // 小部件本体：按源尺寸布局一次，只平移，内容不重排
        Box(
            modifier = Modifier
                .width(with(densityObj) { sourceW.toDp() })
                .height(with(densityObj) { sourceH.toDp() })
                .graphicsLayer {
                    val p = move.value
                    translationX = sourceRect.left + (targetLeft - sourceRect.left) * p
                    translationY = sourceRect.top + (targetTop - sourceRect.top) * p -
                        sheetShift.value * p
                }
                .pointerInput(wallpaper, sourceW, sourceH) {
                    val bitmap = wallpaper
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        if (bitmap == null) {
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                            return@awaitEachGesture
                        }
                        do {
                            val event = awaitPointerEvent()
                            // 焦点 + 缩放，按容器尺寸现算
                            wallpaperCrop = updateWidgetCropForGesture(
                                crop = wallpaperCrop,
                                pan = event.calculatePan(),
                                zoom = event.calculateZoom(),
                                bitmapW = bitmap.width,
                                bitmapH = bitmap.height,
                                containerW = sourceW,
                                containerH = sourceH,
                            )
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            WidgetPreviewContent(
                page = request.page,
                fillBounds = true,
                appearance = appearance,
                wallpaper = wallpaper?.let { WidgetWallpaper(it, wallpaperCrop) },
                isDark = remember(wallpaper, themeMode) {
                    resolveWidgetDark(wallpaper, themeMode, appIsDark)
                },
            )
        }

        // 顶栏：取消 / 应用
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .graphicsLayer { alpha = scrim.value },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreviewPill(
                modifier = Modifier.width(84.dp),
                containerColor = Color(0xFF5C5C5C),
                enabled = !closing,
                onClick = { cancel() },
            ) {
                Text(
                    text = "取消",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            PreviewPill(
                modifier = Modifier.width(84.dp),
                containerColor = primaryColor,
                enabled = !closing && !saving,
                onClick = { apply() },
            ) {
                Text(
                    text = "应用",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 底部工具栏：小部件只要壁纸 + 自定义
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                // 距底**固定** 80dp
                .padding(bottom = 80.dp)
                .graphicsLayer {
                    alpha = toolAlpha.value
                    translationY = toolSlide.value * 40.dp.toPx()
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasWallpaper = wallpaper != null
            ToolCircle(
                icon = if (hasWallpaper) Close else Image,
                contentDescription = if (hasWallpaper) "清除壁纸" else "换壁纸",
                containerColor = if (hasWallpaper) WidgetCustomizeClearColor else WidgetCustomizeToolColor,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    if (hasWallpaper) {
                        wallpaper = null
                        wallpaperCrop = WidgetWallpaperCrop()
                        wallpaperDirty = true
                        if (autoTheme) themeMode = DisplaySettingsPort.THEME_MODE_SYSTEM
                    } else {
                        showPicker = true
                    }
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(width = 2.dp, height = 36.dp)
                    .clip(ContinuousCapsule())
                    .background(WidgetCustomizeToolColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
            ToolCircle(
                icon = GridView,
                contentDescription = "自定义",
                containerColor = WidgetCustomizeToolColor,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    showSheet = true
                }
            )
        }

        // 面板升起时小部件整体上移，落在「面板之上那半屏」的正中
        LaunchedEffect(showSheet, sheetHeightPx) {
            val target = if (showSheet) sheetHeightPx / 2f else 0f
            sheetShift.animateTo(target, tween(320, easing = CustomizePageEasing))
        }

        BlurBottomSheet(
            show = showSheet,
            title = "自定义",
            onDismissRequest = { showSheet = false },
            startAction = { SheetCloseButton(onDismissRequest = { showSheet = false }) },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .onSizeChanged { sheetHeightPx = it.height.toFloat() }
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(58.dp))
                SheetCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "默认主题",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WidgetThemeOptions.forEach { mode ->
                                val selected = themeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(ContinuousCapsule())
                                        .background(
                                            if (selected) MiuixTheme.colorScheme.primary
                                            else MiuixTheme.colorScheme.surfaceContainerHighest
                                        )
                                        .clickable(
                                            interactionSource = null,
                                            indication = null,
                                            role = Role.Button,
                                            onClick = {
                                                themeMode = mode
                                                autoTheme = false
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = widgetThemeLabel(mode),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) Color.White
                                        else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                SheetCard {
                    EffectSlider(
                        label = "卡片字号",
                        value = appearance.textScale,
                        valueRange = WidgetTextScaleRange,
                        keyPoints = listOf(1f),
                        displayValue = { (it * 100).roundToInt().toString() },
                        suffix = "%",
                        quantize = { (it * 20f).roundToInt() / 20f },
                        onValueChange = { appearance = appearance.copy(textScale = it) },
                    )
                    EffectSlider(
                        label = "壁纸模糊",
                        value = appearance.wallpaperBlur,
                        valueRange = WidgetWallpaperBlurRange,
                        keyPoints = listOf(WIDGET_WALLPAPER_BLUR_DEFAULT),
                        enabled = wallpaper != null,
                        displayValue = { it.roundToInt().toString() },
                        suffix = "dp",
                        quantize = { it.roundToInt().toFloat() },
                        onValueChange = { appearance = appearance.copy(wallpaperBlur = it) },
                    )
                }
                Spacer(Modifier.height(sheetContentBottomInset()))
            }
        }

        // 选图：不裁切、拿原图 uri，取景交给浮层里的手势
        pictureSelectorHost(
            showPicker,
            { showPicker = false },
            { uri ->
                showPicker = false
                scope.launch {
                    val decoded: ImageBitmap? = runCatching {
                        withContext(restarhalf.stellar.schedule.platform.AppIoDispatcher) {
                            WallpaperStore.backend?.decodeUri(uri)
                        }
                    }.getOrNull()
                    if (decoded != null) {
                        wallpaper = decoded
                        wallpaperCrop = WidgetWallpaperCrop()
                        wallpaperDirty = true
                        // 选完图，深浅就按照片明暗自动定下来
                        themeMode = resolveThemeMode(
                            DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER,
                            decoded,
                            appIsDark,
                        )
                        autoTheme = true
                    }
                }
            },
            null,
            null,
        )
    }
}

/**
 * 主题档位折算：旧档位 / 默认的「跟随壁纸」按当前照片明暗折算成深色或浅色，
 * 保证三段里必有一段选中。没有壁纸时退回「跟随系统」。
 */
internal fun resolveThemeMode(stored: Int, bitmap: ImageBitmap?, appDark: Boolean): Int =
    if (stored != DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER) {
        stored
    } else if (bitmap == null) {
        DisplaySettingsPort.THEME_MODE_SYSTEM
    } else if (resolveWidgetDark(bitmap, DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER, appDark)) {
        DisplaySettingsPort.THEME_MODE_DARK
    } else {
        DisplaySettingsPort.THEME_MODE_LIGHT
    }
