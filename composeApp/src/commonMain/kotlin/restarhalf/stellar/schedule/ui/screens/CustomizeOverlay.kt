package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.domain.model.CardEdgeThickness
import restarhalf.stellar.schedule.domain.model.CardRefractionLevel
import restarhalf.stellar.schedule.domain.policy.WallpaperTransform
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.ui.chrome.LocalSystemBarsDarkOverride
import restarhalf.stellar.schedule.ui.icons.Close
import restarhalf.stellar.schedule.ui.icons.Examination
import restarhalf.stellar.schedule.ui.icons.GridView
import restarhalf.stellar.schedule.ui.icons.Home
import restarhalf.stellar.schedule.ui.icons.Image
import restarhalf.stellar.schedule.ui.icons.PE
import restarhalf.stellar.schedule.ui.icons.Schedule
import restarhalf.stellar.schedule.ui.components.GlassDropdownMenu
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import restarhalf.stellar.schedule.ui.components.SheetCloseButton
import restarhalf.stellar.schedule.ui.components.sheetContentBottomInset
import restarhalf.stellar.schedule.ui.icons.Tune
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.BlurBottomSheet
import top.yukonga.miuix.kmp.overlay.LocalBlurBottomSheetContentExpanded
import top.yukonga.miuix.kmp.overlay.LocalSheetContentBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.math.roundToInt

internal val CustomizePageEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

private val PreviewThemeOptions = persistentListOf("跟随系统", "浅色", "深色", "跟随壁纸")

private const val PAGE_BACKGROUND = 0xFF1A1A1A
private const val TOOL_BACKGROUND = 0xFF363636
private const val CLEAR_WALLPAPER_BACKGROUND = 0xFFB71C1C

@Immutable
data class CustomizeDraft(
    val brightness: Float,
    val blur: Float,
    val backgroundAlpha: Float,
    val componentsAlpha: Float,
    val wallpaperOffset: Offset,
    val wallpaperScale: Float,
    val cardBlurPx: Float = BackgroundSettingsPort.DEFAULT_CARD_BLUR_PX,
    val cardRefraction: CardRefractionLevel = CardRefractionLevel.DEFAULT,
    val cardEdgeThickness: CardEdgeThickness = CardEdgeThickness.DEFAULT,
    val cardRim: Boolean = BackgroundSettingsPort.DEFAULT_CARD_RIM,
)

@Composable
fun CustomizeOverlay(
    vm: BackgroundViewModel,
    scaleProvider: () -> Float,
    offsetYPxProvider: () -> Float,
    selectedPage: Int,
    onSelectPage: (Int) -> Unit,
    onSheetOpenChange: (Boolean) -> Unit,
    onDraftChange: (CustomizeDraft) -> Unit,
    exiting: Boolean,
    onRequestClose: (apply: Boolean) -> Unit,
    pictureSelectorHost: @Composable (show: Boolean, onDismissRequest: () -> Unit, onPicked: (String) -> Unit, outputWidthPx: Int?, outputHeightPx: Int?) -> Unit = { _, _, _, _, _ -> },
    themeMode: Int = 0,
    onThemeModeChange: (Int) -> Unit = {},
    showNonCurrentWeek: Boolean = false,
    onShowNonCurrentWeekChange: (Boolean) -> Unit = {},
    rowHeightDp: Int = DisplaySettingsPort.DEFAULT_ROW_HEIGHT_DP,
    rowHeightRange: IntRange = 48..120,
    onRowHeightChange: (Int) -> Unit = {},
) {
    val backgroundUiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = MiuixTheme.colorScheme

    val systemBarsDarkOverride = LocalSystemBarsDarkOverride.current
    DisposableEffect(Unit) {
        systemBarsDarkOverride.value = true
        onDispose { systemBarsDarkOverride.value = null }
    }

    val hasWallpaper = backgroundUiState.wallpaper != null

    var brightness by remember { mutableFloatStateOf(backgroundUiState.backgroundBrightness) }
    var blur by remember { mutableFloatStateOf(backgroundUiState.backgroundBlur) }
    var backgroundAlpha by remember { mutableFloatStateOf(backgroundUiState.backgroundAlpha) }
    var componentsAlpha by remember { mutableFloatStateOf(backgroundUiState.componentsAlpha) }
    var wallpaperOffset by remember { mutableStateOf(backgroundUiState.wallpaperOffset) }
    var wallpaperScale by remember { mutableFloatStateOf(backgroundUiState.wallpaperScale) }
    var cardBlurPx by remember { mutableFloatStateOf(backgroundUiState.cardBlurPx) }
    var cardRefraction by remember { mutableStateOf(backgroundUiState.cardRefraction) }
    var cardEdgeThickness by remember { mutableStateOf(backgroundUiState.cardEdgeThickness) }
    var cardRim by remember { mutableStateOf(backgroundUiState.cardRim) }

    LaunchedEffect(backgroundUiState.wallpaper, backgroundUiState.wallpaperOffset, backgroundUiState.wallpaperScale) {
        wallpaperOffset = backgroundUiState.wallpaperOffset
        wallpaperScale = backgroundUiState.wallpaperScale
    }
    LaunchedEffect(
        brightness,
        blur,
        backgroundAlpha,
        componentsAlpha,
        wallpaperOffset,
        wallpaperScale,
        cardBlurPx,
        cardRefraction,
        cardEdgeThickness,
        cardRim,
    ) {
        onDraftChange(
            CustomizeDraft(
                brightness = brightness,
                blur = blur,
                backgroundAlpha = backgroundAlpha,
                componentsAlpha = componentsAlpha,
                wallpaperOffset = wallpaperOffset,
                wallpaperScale = wallpaperScale,
                cardBlurPx = cardBlurPx,
                cardRefraction = cardRefraction,
                cardEdgeThickness = cardEdgeThickness,
                cardRim = cardRim,
            ),
        )
    }

    var showEffects by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var showPictureSelector by remember { mutableStateOf(false) }

    val chromeAlpha = remember { Animatable(0f) }
    val buttonScale = remember { Animatable(1.35f) }
    val toolProgress = remember { Animatable(0f) }
    val thumbProgress = remember { Animatable(selectedPage.toFloat()) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { chromeAlpha.animateTo(1f, tween(320)) }
            launch { buttonScale.animateTo(1f, tween(380, easing = CustomizePageEasing)) }
            launch { toolProgress.animateTo(1f, tween(380, easing = CustomizePageEasing)) }
        }
    }

    LaunchedEffect(selectedPage) {
        thumbProgress.animateTo(
            targetValue = selectedPage.toFloat(),
            animationSpec = tween(280, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)),
        )
    }

    val anySheetOpen = showEffects || showCustomize
    LaunchedEffect(anySheetOpen) {
        onSheetOpenChange(anySheetOpen)
        if (exiting) return@LaunchedEffect
        coroutineScope {
            launch { chromeAlpha.animateTo(if (anySheetOpen) 0f else 1f, tween(250)) }
        }
    }

    LaunchedEffect(exiting) {
        if (exiting) {
            showEffects = false
            showCustomize = false
            coroutineScope {
                launch { chromeAlpha.animateTo(0f, tween(220)) }
                launch { toolProgress.animateTo(0f, tween(220)) }
            }
        }
    }

    val chromeInteractable by remember(chromeAlpha) { derivedStateOf { chromeAlpha.value > 0.5f } }

    Scaffold(containerColor = Color.Transparent) { _ ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidthPx = constraints.maxWidth.toFloat()
            val screenHeightPx = constraints.maxHeight.toFloat()
            val cutoutCache = remember { CutoutPathCache() }

            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                } while (event.changes.any { it.pressed })
                            }
                        },
            ) {
                val scale = scaleProvider()
                val offsetYPx = offsetYPxProvider()
                val scrim = cutoutCache.obtain(
                    width = size.width,
                    height = size.height,
                    scale = scale,
                    offsetYPx = offsetYPx,
                    minCornerPx = 20.dp.toPx(),
                    baseCornerPx = 40.dp.toPx(),
                )
                drawPath(scrim, Color(PAGE_BACKGROUND))
            }

            backgroundUiState.wallpaper?.let { bitmap ->
                WallpaperGestureLayer(
                    wallpaper = bitmap,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    scaleProvider = scaleProvider,
                    offsetYPxProvider = offsetYPxProvider,
                    offset = wallpaperOffset,
                    wallpaperScale = wallpaperScale,
                    onTransform = { newOffset, newScale ->
                        wallpaperOffset = newOffset
                        wallpaperScale = newScale
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .padding(start = 20.dp, end = 20.dp, top = statusBarTopPadding() + 16.dp)
                        .graphicsLayer { alpha = chromeAlpha.value },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PreviewPill(
                    modifier =
                        Modifier
                            .width(84.dp)
                            .graphicsLayer {
                                scaleX = buttonScale.value
                                scaleY = buttonScale.value
                            },
                    containerColor = Color(TOOL_BACKGROUND).copy(alpha = 0.8f),
                    enabled = chromeInteractable,
                    onClick = { onRequestClose(false) },
                ) {
                    Text(
                        text = "取消",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                PreviewPill(
                    modifier =
                        Modifier
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = buttonScale.value
                                scaleY = buttonScale.value
                            },
                    containerColor = Color(TOOL_BACKGROUND).copy(alpha = 0.8f),
                    enabled = chromeInteractable,
                    onClick = {},
                    contentPadding = 3.dp,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.25f)
                                    .graphicsLayer { translationX = thumbProgress.value * size.width }
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.1f)),
                        )
                        Row(modifier = Modifier.fillMaxSize()) {
                            val tabs =
                                listOf(
                                    Triple(Home, "今日", 0),
                                    Triple(Schedule, "课表", 1),
                                    Triple(Examination, "考务", 2),
                                    Triple(PE, "体测", 3),
                                )
                            tabs.forEach { (icon, description, index) ->
                                PreviewPageTab(
                                    icon = icon,
                                    description = description,
                                    selected = selectedPage == index,
                                    enabled = chromeInteractable,
                                    onClick = { onSelectPage(index) },
                                )
                            }
                        }
                    }
                }

                PreviewPill(
                    modifier =
                        Modifier
                            .width(84.dp)
                            .graphicsLayer {
                                scaleX = buttonScale.value
                                scaleY = buttonScale.value
                            },
                    containerColor = colors.primary,
                    enabled = chromeInteractable,
                    onClick = { onRequestClose(true) },
                ) {
                    Text(
                        text = "应用",
                        color = colors.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                            ),
                        )
                        .padding(bottom = 20.dp)
                        .graphicsLayer {
                            alpha = toolProgress.value
                            translationY = (1f - toolProgress.value) * 80.dp.toPx()
                        },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolCircle(
                    icon = if (hasWallpaper) Close else Image,
                    contentDescription = if (hasWallpaper) "清除壁纸" else "换壁纸",
                    containerColor = Color(if (hasWallpaper) CLEAR_WALLPAPER_BACKGROUND else TOOL_BACKGROUND),
                    onClick = {
                        if (hasWallpaper) vm.onWallpaperCleared() else showPictureSelector = true
                    },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(Color(TOOL_BACKGROUND)),
                )
                Spacer(modifier = Modifier.width(4.dp))
                ToolCircle(
                    icon = Tune,
                    contentDescription = "效果",
                    containerColor = Color(TOOL_BACKGROUND),
                    onClick = { showEffects = true },
                )
                Spacer(modifier = Modifier.width(10.dp))
                ToolCircle(
                    icon = GridView,
                    contentDescription = "自定义",
                    containerColor = Color(TOOL_BACKGROUND),
                    onClick = { showCustomize = true },
                )
            }
        }

        val defaultSheetContentHeight =
            with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() * 0.52f }

        BlurBottomSheet(
            show = showEffects,
            title = "效果",
            onDismissRequest = { showEffects = false },
            startAction = { SheetCloseButton(onDismissRequest = { showEffects = false }) },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (LocalBlurBottomSheetContentExpanded.current) {
                                Modifier.fillMaxHeight()
                            } else {
                                Modifier.height(defaultSheetContentHeight)
                            },
                        )
                        .overScrollVertical()
                        .scrollEndHaptic(HapticFeedbackType.TextHandleMove)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(58.dp))
                val sheetBackdrop = LocalSheetContentBackdrop.current
                val glassDropdownColors =
                    DropdownDefaults.dropdownColors(
                        containerColor = Color.Transparent,
                        selectedContainerColor = Color.Transparent,
                    )
                SheetCard {
                    GlassDropdownMenu(
                        title = "默认主题",
                        summary = "深色/浅色可跟随系统或壁纸",
                        entry =
                            DropdownEntry(
                                items =
                                    PreviewThemeOptions.mapIndexed { index, text ->
                                        DropdownItem(
                                            text = text,
                                            selected = index == themeMode,
                                            onClick = { onThemeModeChange(index) },
                                        )
                                    },
                            ),
                        dropdownColors = glassDropdownColors,
                        liquidGlassBackdrop = sheetBackdrop,
                    )
                }
                SheetCard {
                    EffectSlider(
                        label = "壁纸亮度",
                        value = brightness,
                        valueRange = -50f..50f,
                        keyPoints = listOf(0f),
                        displayValue = { it.roundToInt().let { v -> if (v > 0) "+$v" else "$v" } },
                        enabled = hasWallpaper,
                        onValueChange = { brightness = it },
                    )
                    EffectSlider(
                        label = "壁纸模糊",
                        value = blur,
                        valueRange = 0f..1f,
                        keyPoints = listOf(0f),
                        displayValue = { "${(it * 100).roundToInt()}" },
                        suffix = "%",
                        enabled = hasWallpaper,
                        onValueChange = { blur = it },
                    )
                    EffectSlider(
                        label = "背景不透明度",
                        value = backgroundAlpha,
                        valueRange = 0f..1f,
                        keyPoints = listOf(1f),
                        displayValue = { "${(it * 100).roundToInt()}" },
                        suffix = "%",
                        enabled = hasWallpaper,
                        onValueChange = { backgroundAlpha = it },
                    )
                }
                SheetCard {
                    EffectSlider(
                        label = "组件不透明度",
                        value = componentsAlpha,
                        valueRange = 0f..1f,
                        keyPoints = listOf(BackgroundSettingsPort.DEFAULT_COMPONENTS_ALPHA),
                        displayValue = { "${(it * 100).roundToInt()}" },
                        suffix = "%",
                        enabled = hasWallpaper,
                        onValueChange = { componentsAlpha = it },
                    )
                }
                val glassAvailable = AppMaterialSettings.glassAvailable()
                val glassDisabledText = if (!glassAvailable) "无材质" else "需设置壁纸"
                val glassItemEnabled = hasWallpaper && glassAvailable
                SheetCard {
                    EffectSlider(
                        label = "卡片模糊",
                        value = cardBlurPx,
                        valueRange = 0f..BackgroundSettingsPort.MAX_CARD_BLUR_PX,
                        keyPoints = listOf(BackgroundSettingsPort.DEFAULT_CARD_BLUR_PX),
                        displayValue = { it.roundToInt().toString() },
                        suffix = "px",
                        enabled = glassItemEnabled,
                        disabledText = glassDisabledText,
                        quantize = { it.roundToInt().coerceIn(0, 24).toFloat() },
                        onValueChange = { if (hasWallpaper) cardBlurPx = it },
                    )
                    RefractionSlider(
                        value = cardRefraction,
                        enabled = glassItemEnabled,
                        disabledText = glassDisabledText,
                        onValueChange = { if (hasWallpaper) cardRefraction = it },
                    )
                    EdgeThicknessSlider(
                        value = cardEdgeThickness,
                        enabled = glassItemEnabled,
                        disabledText = glassDisabledText,
                        onValueChange = { if (hasWallpaper) cardEdgeThickness = it },
                    )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "高光描边",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (glassItemEnabled) colors.onSurface else colors.onSurface.copy(alpha = 0.4f),
                        )
                        Switch(
                            checked = cardRim,
                            enabled = glassItemEnabled,
                            onCheckedChange = { if (hasWallpaper) cardRim = it },
                        )
                    }
                }
                Spacer(Modifier.height(sheetContentBottomInset()))
            }
        }

        BlurBottomSheet(
            show = showCustomize,
            title = "自定义",
            onDismissRequest = { showCustomize = false },
            startAction = { SheetCloseButton(onDismissRequest = { showCustomize = false }) },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (LocalBlurBottomSheetContentExpanded.current) {
                                Modifier.fillMaxHeight()
                            } else {
                                Modifier.height(defaultSheetContentHeight)
                            },
                        )
                        .overScrollVertical()
                        .scrollEndHaptic(HapticFeedbackType.TextHandleMove)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(58.dp))
                SheetCard {
                    EffectSlider(
                        label = "课程格子高度",
                        value = rowHeightDp.toFloat(),
                        valueRange = rowHeightRange.first.toFloat()..rowHeightRange.last.toFloat(),
                        keyPoints = listOf(DisplaySettingsPort.DEFAULT_ROW_HEIGHT_DP.toFloat()),
                        displayValue = { it.roundToInt().toString() },
                        suffix = "dp",
                        quantize = { it.roundToInt().toFloat() },
                        onValueChange = { onRowHeightChange(it.roundToInt()) },
                    )
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "显示非本周课程",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface,
                        )
                        Switch(
                            checked = showNonCurrentWeek,
                            onCheckedChange = onShowNonCurrentWeekChange,
                        )
                    }
                }
                Spacer(Modifier.height(sheetContentBottomInset()))
            }
        }

        // 必须放在 Scaffold 内容里：BlurBottomSheet 走 DialogLayout(renderInRootScaffold=true)，
        // 只有在 Scaffold 提供的 LocalRootDialogStates 作用域内注册，才会被其 MiuixPopupHost 渲染。
        // 放在 Scaffold 外面会落进无人渲染的默认 LocalDialogStates，弹窗永远不出现。
        pictureSelectorHost(
            showPictureSelector,
            { showPictureSelector = false },
            { pickedUri ->
                vm.onWallpaperPicked(pickedUri)
                showPictureSelector = false
            },
            null,
            null,
        )
    }
}

/** 复用 outer/hole Path 并按参数缓存挖洞遮罩，静止与拖拽/调参期间零重建、零 PathOperation.Difference。 */
private class CutoutPathCache {
    private var lastWidth = Float.NaN
    private var lastHeight = Float.NaN
    private var lastScale = Float.NaN
    private var lastOffsetYPx = Float.NaN
    private val outerPath = Path()
    private val holePath = Path()
    private var scrimPath = Path()

    fun obtain(
        width: Float,
        height: Float,
        scale: Float,
        offsetYPx: Float,
        minCornerPx: Float,
        baseCornerPx: Float,
    ): Path {
        if (width == lastWidth && height == lastHeight && scale == lastScale && offsetYPx == lastOffsetYPx) {
            return scrimPath
        }
        lastWidth = width
        lastHeight = height
        lastScale = scale
        lastOffsetYPx = offsetYPx
        val holeWidth = width * scale
        val holeHeight = height * scale
        val holeLeft = (width - holeWidth) / 2f
        val holeTop = (height - holeHeight) / 2f + offsetYPx
        val corner = (baseCornerPx * scale).coerceAtLeast(minCornerPx)
        outerPath.rewind()
        outerPath.addRect(Rect(0f, 0f, width, height))
        holePath.rewind()
        holePath.addRoundRect(
            RoundRect(
                rect = Rect(holeLeft, holeTop, holeLeft + holeWidth, holeTop + holeHeight),
                cornerRadius = CornerRadius(corner, corner),
            ),
        )
        val result = Path()
        if (!result.op(outerPath, holePath, PathOperation.Difference)) {
            result.addPath(outerPath)
            result.addPath(holePath)
            result.fillType = PathFillType.EvenOdd
        }
        scrimPath = result
        return result
    }
}

@Composable
private fun WallpaperGestureLayer(
    wallpaper: ImageBitmap,
    screenWidthPx: Float,
    screenHeightPx: Float,
    scaleProvider: () -> Float,
    offsetYPxProvider: () -> Float,
    offset: Offset,
    wallpaperScale: Float,
    onTransform: (Offset, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestOffset by rememberUpdatedState(offset)
    val latestScale by rememberUpdatedState(wallpaperScale)
    val latestOnTransform by rememberUpdatedState(onTransform)
    val minScale =
        remember(wallpaper, screenWidthPx, screenHeightPx) {
            WallpaperTransform.minScale(wallpaper.width, wallpaper.height, screenWidthPx, screenHeightPx)
        }

    Box(
        modifier =
            modifier
                .layout { measurable, constraints ->
                    val cardScale = scaleProvider().coerceAtLeast(0.01f)
                    val w = (constraints.maxWidth * cardScale).roundToInt().coerceAtLeast(1)
                    val h = (constraints.maxHeight * cardScale).roundToInt().coerceAtLeast(1)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(w, h) {
                        placeable.place(0, offsetYPxProvider().roundToInt())
                    }
                }
                .pointerInput(wallpaper, screenWidthPx, screenHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        var gestureScale = maxOf(latestScale, minScale)
                        var previewScale = latestScale
                        var previewOffset = latestOffset
                        do {
                            val event = awaitPointerEvent()
                            gestureScale *= event.calculateZoom()
                            val newScale =
                                if (gestureScale < minScale) {
                                    minScale + (gestureScale - minScale) * 0.3f
                                } else {
                                    gestureScale
                                }
                            val (maxOffsetX, maxOffsetY) =
                                WallpaperTransform.maxOffset(
                                    bitmapW = wallpaper.width,
                                    bitmapH = wallpaper.height,
                                    screenW = screenWidthPx,
                                    screenH = screenHeightPx,
                                    scale = newScale,
                                )
                            val cardScale = scaleProvider().coerceAtLeast(0.01f)
                            val newOffset = previewOffset + event.calculatePan() / cardScale
                            previewScale = newScale
                            previewOffset =
                                Offset(
                                    newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                    newOffset.y.coerceIn(-maxOffsetY, maxOffsetY),
                                )
                            latestOnTransform(previewOffset, previewScale)
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                    }
                },
    )
}

@Composable
private fun statusBarTopPadding(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

@Composable
internal fun PreviewPill(
    modifier: Modifier,
    containerColor: Color,
    onClick: () -> Unit,
    contentPadding: Dp = 0.dp,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier =
            modifier
                .height(40.dp)
                .clip(shape)
                .background(containerColor)
                .border(0.5.dp, Color.White.copy(alpha = 0.1f), shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PreviewPageTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color.White.copy(alpha = if (selected) 1f else 0.75f),
        )
    }
}

@Composable
internal fun ToolCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
internal fun SheetCard(content: @Composable ColumnScope.() -> Unit) {
    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        colors =
            CardDefaults.defaultColors(
                color =
                    if (MiuixTheme.isDynamicColor) {
                        MiuixTheme.colorScheme.surfaceContainerHigh
                    } else if (isDark) {
                        Color(0xFF303030)
                    } else {
                        Color(0xFFFFFFFF)
                    },
                contentColor = MiuixTheme.colorScheme.onSurface,
            ),
        content = content,
    )
}

@Composable
internal fun EffectSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    keyPoints: List<Float>,
    displayValue: (Float) -> String,
    suffix: String = "",
    enabled: Boolean = true,
    disabledText: String = "需设置壁纸",
    quantize: (Float) -> Float = { it },
    onValueChange: (Float) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val textColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.4f)
    var rawValue by remember { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!isDragging) rawValue = value
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f),
            )
            if (enabled) {
                Text(
                    text = displayValue(value) + suffix,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(text = disabledText, fontSize = 14.sp, color = textColor)
            }
        }
        Slider(
            value = rawValue,
            onValueChange = { raw ->
                isDragging = true
                rawValue = raw
                onValueChange(quantize(raw))
            },
            onValueChangeFinished = {
                isDragging = false
                rawValue = quantize(rawValue)
            },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            showKeyPoints = true,
            keyPoints = keyPoints,
            magnetThreshold = 0.05f,
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
        )
    }
}

@Composable
private fun RefractionSlider(
    value: CardRefractionLevel,
    enabled: Boolean,
    disabledText: String,
    onValueChange: (CardRefractionLevel) -> Unit,
) {
    DiscreteEnumSlider(
        label = "卡片折射",
        entries = CardRefractionLevel.entries,
        selectedOrdinal = value.ordinal,
        selectedLabel = value.label,
        enabled = enabled,
        disabledText = disabledText,
        onSelectOrdinal = { onValueChange(CardRefractionLevel.fromOrdinal(it)) },
    )
}

@Composable
private fun EdgeThicknessSlider(
    value: CardEdgeThickness,
    enabled: Boolean,
    disabledText: String,
    onValueChange: (CardEdgeThickness) -> Unit,
) {
    DiscreteEnumSlider(
        label = "边缘厚度",
        entries = CardEdgeThickness.entries,
        selectedOrdinal = value.ordinal,
        selectedLabel = value.label,
        enabled = enabled,
        disabledText = disabledText,
        onSelectOrdinal = { onValueChange(CardEdgeThickness.fromOrdinal(it)) },
    )
}

@Composable
private fun DiscreteEnumSlider(
    label: String,
    entries: List<*>,
    selectedOrdinal: Int,
    selectedLabel: String,
    enabled: Boolean,
    disabledText: String,
    onSelectOrdinal: (Int) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val lastIndex = entries.lastIndex.coerceAtLeast(0)
    val textColor = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.4f)
    var rawValue by remember { mutableFloatStateOf(selectedOrdinal.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    LaunchedEffect(selectedOrdinal) {
        if (!isDragging) rawValue = selectedOrdinal.toFloat()
    }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 2.dp, end = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f),
            )
            if (enabled) {
                Text(
                    text = selectedLabel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(text = disabledText, fontSize = 14.sp, color = textColor)
            }
        }
        Slider(
            value = rawValue,
            onValueChange = { v ->
                isDragging = true
                rawValue = v
                onSelectOrdinal(v.roundToInt().coerceIn(0, lastIndex))
            },
            onValueChangeFinished = {
                isDragging = false
                rawValue = rawValue.roundToInt().coerceIn(0, lastIndex).toFloat()
            },
            valueRange = 0f..lastIndex.toFloat(),
            showKeyPoints = true,
            keyPoints = entries.indices.map { it.toFloat() },
            magnetThreshold = 0.01f,
            modifier = Modifier.fillMaxWidth(),
            hapticEffect = SliderDefaults.SliderHapticEffect.Step,
            enabled = enabled,
        )
    }
}
