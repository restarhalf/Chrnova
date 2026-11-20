package restarhalf.stellar.schedule.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chrnova.composeapp.generated.resources.Res
import chrnova.composeapp.generated.resources.ic_widget_clock
import chrnova.composeapp.generated.resources.ic_widget_location
import com.haooz.softglass.shape.ContinuousCapsule
import com.haooz.softglass.shape.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import restarhalf.stellar.schedule.data.wallpaper.WIDGET_CARD_ALPHA_WITH_WALLPAPER
import restarhalf.stellar.schedule.data.wallpaper.WallpaperStore
import restarhalf.stellar.schedule.data.wallpaper.WidgetAppearance
import restarhalf.stellar.schedule.data.wallpaper.WidgetWallpaperCrop
import restarhalf.stellar.schedule.data.wallpaper.WidgetWallpaperStore
import restarhalf.stellar.schedule.data.wallpaper.calculateWidgetCropRect
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort
import restarhalf.stellar.schedule.ui.chrome.AppPageScaffold
import restarhalf.stellar.schedule.ui.chrome.appPageContentPadding
import restarhalf.stellar.schedule.ui.chrome.pageScrollModifiers
import restarhalf.stellar.schedule.ui.components.AppCard
import restarhalf.stellar.schedule.ui.components.GlassDropdownMenu
import restarhalf.stellar.schedule.ui.components.LocalLiquidGlassBackdrop
import restarhalf.stellar.schedule.ui.icons.GridView
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.layout.collapsibleTopInset
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.LocalAppSettingDark
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** 4×2 组件预览统一宽高比，锁定后不随设备屏宽变化 */
private const val WidgetPreviewRatio4x2 = 2.15f

/** 预览卡上下的内边距（dp） */
private val WidgetPreviewVerticalPadding = 12.dp

/** 翻转卡最大模糊半径（dp），翻转进度越过起始阈值后 0 → 此值线性渐入 */
private val WidgetPreviewFlipBlurRadius = 8.dp

/** 模糊起始阈值：翻转进度（0–1）小于此值完全清晰，越过后才开始加模糊 */
private const val WidgetPreviewBlurStartProgress = 0.12f

/** 模糊出血（dp）：RenderEffect 会把图层强制裁到自身边界 */
private val WidgetPreviewBlurBleed = 8.dp

/**
 * 点「自定义」时交给浮层的现场信息。
 *
 * @param page 预览页序号（0=今日 4×2，1=今明 4×2，2=2×2）
 * @param bounds 该小部件此刻在屏幕（root）上的矩形。浮层从这里起步。
 */
data class WidgetCustomizeRequest(
    val page: Int,
    val bounds: Rect,
)

/**
 * 桌面小部件引导路由。
 *
 * @param onRequestWidgetRefresh Android 注入「清签名缓存 + 刷新已安装小部件」；
 *   iOS 没有 AppWidget 概念，默认空实现。
 */
@Composable
fun WidgetIntroRoute(
    onBack: () -> Unit,
    pictureSelectorHost: @Composable (
        show: Boolean,
        onDismissRequest: () -> Unit,
        onPicked: (String) -> Unit,
        outputWidthPx: Int?,
        outputHeightPx: Int?,
    ) -> Unit = { _, _, _, _, _ -> },
    onRequestWidgetRefresh: () -> Unit = {},
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var showGuideDialog by remember { mutableStateOf(false) }
    // 自定义浮层铺满整屏（含状态栏），所以挂在脚手架外侧
    var customizeRequest by remember { mutableStateOf<WidgetCustomizeRequest?>(null) }
    // 浮层已把小部件送回原位（但还没摘）：页面那份预览此时可以放出来了
    var customizeSettled by remember { mutableStateOf(false) }
    // 「应用」后自增，逼页面重读壁纸
    var wallpaperRevision by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        AppPageScaffold(
            title = "桌面小部件",
            onBack = onBack,
            bottomBar = {
                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        showGuideDialog = true
                    },
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    cornerRadius = 20.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 25.dp, start = 10.dp, end = 10.dp),
                ) {
                    Text(text = "添加到桌面", color = MiuixTheme.colorScheme.onPrimary)
                }
            },
            popupHost = {
                OverlayDialog(
                    show = showGuideDialog,
                    liquidGlassBackdrop = LocalLiquidGlassBackdrop.current,
                    title = "添加桌面小部件",
                    onDismissRequest = { showGuideDialog = false },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "1. 长按桌面空白处\n2. 选择「全部应用」内的「安卓小部件」\n3. 找到「Chrnova」并添加",
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                showGuideDialog = false
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "知道了", color = MiuixTheme.colorScheme.onPrimary)
                        }
                    }
                }
            },
        ) { scrollBehavior, innerPadding ->
            WidgetIntroScreen(
                scrollBehavior = scrollBehavior,
                innerPadding = innerPadding,
                onCustomizeRequest = { customizeRequest = it },
                // 浮层在画这张小部件，页面里那份必须让位；
                // 但「已回位、还没摘」的那一刻要放出来 —— 否则摘浮层那帧是空的
                hiddenPage = if (customizeSettled) null else customizeRequest?.page,
                wallpaperRevision = wallpaperRevision,
            )
        }

        customizeRequest?.let { request ->
            WidgetCustomizeOverlay(
                request = request,
                onDismiss = {
                    customizeRequest = null
                    customizeSettled = false
                },
                onApply = {
                    customizeRequest = null
                    customizeSettled = false
                    // 桌面组件刷新是同步重活，留在这一帧里做会把「摘浮层」那帧的绘制顶掉
                    scope.launch {
                        withFrameNanos { }
                        onRequestWidgetRefresh()
                    }
                },
                // 页面先亮起来（和浮层那份完全重合），顺便读到刚落盘的外观
                onSettled = {
                    wallpaperRevision++
                    customizeSettled = true
                },
                pictureSelectorHost = pictureSelectorHost,
            )
        }
    }
}

@Composable
private fun WidgetIntroScreen(
    scrollBehavior: top.yukonga.miuix.kmp.layout.SharedScrollBehavior,
    innerPadding: PaddingValues,
    onCustomizeRequest: (WidgetCustomizeRequest) -> Unit,
    hiddenPage: Int?,
    wallpaperRevision: Int,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val pageCount = 3
    // 每页小部件本体的屏幕矩形，供浮层从原位起步。刻意不用 snapshot state。
    val widgetBounds = remember { mutableMapOf<Int, Rect>() }

    // 各页壁纸：浮层里「应用」后靠 wallpaperRevision 触发重读
    val widgetWallpapers = remember { mutableStateMapOf<Int, WidgetWallpaper?>() }
    val widgetThemeModes = remember { mutableStateMapOf<Int, Int>() }
    val widgetAppearances = remember { mutableStateMapOf<Int, WidgetAppearance>() }
    // 组合期同步刷一遍：应用后 save 已经把新位图放进缓存，这一帧就能拿到。
    remember(wallpaperRevision, pageCount) {
        for (page in 0 until pageCount) {
            val key = widgetWallpaperKey(page)
            widgetWallpapers[page] = WidgetWallpaperStore.cached(key)?.let { cached ->
                WidgetWallpaper(bitmap = cached, crop = WidgetWallpaperStore.loadCrop(key))
            }
            widgetThemeModes[page] = WidgetWallpaperStore.loadThemeMode(key)
            widgetAppearances[page] = WidgetWallpaperStore.loadAppearance(key)
        }
    }
    LaunchedEffect(wallpaperRevision, pageCount) {
        // 冷启动才需要读盘：缓存命中的那几页上面已经填好了
        for (page in 0 until pageCount) {
            val key = widgetWallpaperKey(page)
            if (WidgetWallpaperStore.cached(key) != null) continue
            if (!WidgetWallpaperStore.exists(key)) {
                widgetWallpapers[page] = null
                continue
            }
            val bitmap = WidgetWallpaperStore.load(key, forceReload = true)
            widgetWallpapers[page] = bitmap?.let { bmp ->
                WidgetWallpaper(bitmap = bmp, crop = WidgetWallpaperStore.loadCrop(key))
            }
            widgetThemeModes[page] = WidgetWallpaperStore.loadThemeMode(key)
        }
    }
    val appIsDark = isAppDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pageScrollModifiers(scrollBehavior = scrollBehavior)
            .collapsibleTopInset(scrollBehavior)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val contentPadding = appPageContentPadding(
            innerPadding = innerPadding,
            outerPadding = PaddingValues(0.dp),
            extraTop = 4.dp,
        )
        Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))

        val pagerState = rememberPagerState(pageCount = { pageCount })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 16.dp,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp - WidgetPreviewBlurBleed,
                        vertical = WidgetPreviewVerticalPadding - WidgetPreviewBlurBleed
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            val absOff = min(
                                abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction),
                                1f
                            )
                            cameraDistance = 48f
                            rotationY = -((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction) * 56f
                            val scale = 1f - 0.2f * absOff
                            scaleX = scale
                            scaleY = scale
                            val blurProgress = ((absOff - WidgetPreviewBlurStartProgress) /
                                (1f - WidgetPreviewBlurStartProgress)).coerceIn(0f, 1f)
                            renderEffect = if (blurProgress > 0.01f) {
                                val blur = blurProgress * WidgetPreviewFlipBlurRadius.toPx()
                                BlurEffect(blur, blur)
                            } else null
                        }
                        // 出血必须在 graphicsLayer 内侧：扩大离屏缓冲的采样范围
                        .padding(WidgetPreviewBlurBleed)
                ) {
                    val pageWallpaper = widgetWallpapers[page]
                    val pageMode = widgetThemeModes[page] ?: DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER
                    WidgetPreviewContent(
                        page = page,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                // 只认「小部件本体」的矩形（2×2 是居中的正方形）
                                widgetBounds[page] = coords.boundsInRoot()
                            }
                            .graphicsLayer { alpha = if (page == hiddenPage) 0f else 1f },
                        wallpaper = pageWallpaper,
                        appearance = widgetAppearances[page] ?: WidgetAppearance(),
                        isDark = remember(pageWallpaper, pageMode, appIsDark) {
                            resolveWidgetDark(pageWallpaper?.bitmap, pageMode, appIsDark)
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))

        // 页面指示器（会跟手跑的胶囊，点一下也能跳页）
        WidgetPagerIndicator(pageCount = pageCount, pagerState = pagerState)

        Spacer(modifier = Modifier.height(16.dp))

        // 自定义入口：跟着「当前这一页」的小部件走，所以矩形也取当前页的
        WidgetCustomizeEntry(
            onClick = {
                val page = pagerState.currentPage
                val bounds = widgetBounds[page]
                if (bounds != null) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onCustomizeRequest(WidgetCustomizeRequest(page = page, bounds = bounds))
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            FeatureItem("今日课程一目了然", "桌面显示当天课程、时间、地点和教师信息")
            FeatureItem("今明两天同屏预览", "左右两列分别展示今天与明天，明天列不受提醒开关拦截")
            FeatureItem("进行中课程高亮", "正在上课的课程高亮显示剩余时间")
            FeatureItem("智能显示状态", "开启次日提醒后，当天课全部上完会自动切到明日课程")
            FeatureItem("自由设置外观", "每个小组件可单独设置壁纸、模糊、字号与深浅色")
        }

        Spacer(modifier = Modifier.height(24.dp))

        val displaySettings: DisplaySettingsPort = koinInject()
        var paddingMode by remember { mutableIntStateOf(displaySettings.getWidgetPaddingMode()) }
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            WidgetPaddingSelector(
                selectedMode = paddingMode,
                onSelect = { mode ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    paddingMode = mode
                    displaySettings.setWidgetPaddingMode(mode)
                }
            )
        }

        Spacer(modifier = Modifier.height(contentPadding.calculateBottomPadding()))
        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * 翻页指示器：三颗里有一颗会「跑」。
 * 位置直接取 currentPage + offset，手指拖到哪它就跟到哪；宽度、颜色按重合度逐帧插值。
 */
@Composable
private fun WidgetPagerIndicator(
    pageCount: Int,
    pagerState: androidx.compose.foundation.pager.PagerState,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val activeColor = MiuixTheme.colorScheme.primary
    val inactiveColor = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.12f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .pointerInput(pageCount) {
                detectTapGestures { offset ->
                    val dotPx = IndicatorDot.toPx()
                    val gapPx = IndicatorGap.toPx()
                    val stretchPx = IndicatorStretch.toPx()
                    val position = pagerPosition(pagerState, pageCount)
                    var left = (size.width - indicatorWidth(pageCount, position, dotPx, gapPx, stretchPx)) / 2f
                    for (index in 0 until pageCount) {
                        val width = indicatorSlotWidth(index, position, dotPx, stretchPx)
                        if (offset.x >= left && offset.x <= left + width) {
                            if (index != pagerState.currentPage) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                            break
                        }
                        left += width + gapPx
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotPx = IndicatorDot.toPx()
            val gapPx = IndicatorGap.toPx()
            val stretchPx = IndicatorStretch.toPx()
            val position = pagerPosition(pagerState, pageCount)
            var left = (size.width - indicatorWidth(pageCount, position, dotPx, gapPx, stretchPx)) / 2f
            val centerY = size.height / 2f
            for (index in 0 until pageCount) {
                val weight = indicatorSlotWeight(index, position)
                val width = dotPx + stretchPx * weight
                drawRoundRect(
                    // 只跟「离它多近」有关：路过时颜色一路染过去，停下时正好整颗点亮
                    color = lerp(inactiveColor, activeColor, weight),
                    topLeft = Offset(left, centerY - dotPx / 2f),
                    size = Size(width, dotPx),
                    cornerRadius = CornerRadius(dotPx / 2f),
                )
                left += width + gapPx
            }
        }
    }
}

/** 未选中时的圆点直径 */
private val IndicatorDot = 6.dp

/** 相邻两颗的间隙 */
private val IndicatorGap = 6.dp

/** 选中时额外抻出的长度（加上 [IndicatorDot] 就是胶囊长度） */
private val IndicatorStretch = 12.dp

/** 跟手位置：整页为整数，拖动过程中是小数 */
private fun pagerPosition(
    pagerState: androidx.compose.foundation.pager.PagerState,
    pageCount: Int,
): Float =
    (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (pageCount - 1).coerceAtLeast(0).toFloat())

/** 第 index 颗与当前位置的「重合度」：0 = 完全没挨着，1 = 正好停在它上面 */
private fun indicatorSlotWeight(index: Int, position: Float): Float =
    (1f - abs(position - index)).coerceIn(0f, 1f)

private fun indicatorSlotWidth(
    index: Int,
    position: Float,
    dotPx: Float,
    stretchPx: Float,
): Float = dotPx + stretchPx * indicatorSlotWeight(index, position)

/** 所有圆点 + 间隙的总宽（用来把整排居中） */
private fun indicatorWidth(
    pageCount: Int,
    position: Float,
    dotPx: Float,
    gapPx: Float,
    stretchPx: Float,
): Float {
    if (pageCount <= 0) return 0f
    var total = gapPx * (pageCount - 1)
    for (index in 0 until pageCount) {
        total += indicatorSlotWidth(index, position, dotPx, stretchPx)
    }
    return total
}

@Composable
private fun WidgetCustomizeEntry(onClick: () -> Unit) {
    val contentColor = MiuixTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(ContinuousCapsule())
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        top.yukonga.miuix.kmp.basic.Icon(
            imageVector = GridView,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor,
        )
        Text(
            text = "自定义",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

/** 三种预览页的统一入口：分页器与自定义浮层共用同一份内容，浮层里的小部件才和页面里一模一样 */
@Composable
internal fun WidgetPreviewContent(
    page: Int,
    modifier: Modifier = Modifier,
    /** 传入的尺寸已经是小部件本体时置 true（2×2 不再套 4×2 槽位） */
    fillBounds: Boolean = false,
    wallpaper: WidgetWallpaper? = null,
    appearance: WidgetAppearance = WidgetAppearance(),
    isDark: Boolean = isAppDarkTheme(),
) {
    // 字体缩放靠改 fontScale：一处生效，不用逐个 sp 去乘
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * appearance.textScale)
    ) {
        when (page) {
            0 -> WidgetPreviewToday(modifier, wallpaper, appearance, isDark)
            1 -> WidgetPreviewTodayTomorrow(modifier, wallpaper, appearance, isDark)
            else -> WidgetPreviewSmall2x2(modifier, fillBounds, wallpaper, appearance, isDark)
        }
    }
}

/** 小部件背景图 + 它当前的取景 */
internal data class WidgetWallpaper(
    val bitmap: ImageBitmap,
    val crop: WidgetWallpaperCrop,
)

@Composable
internal fun isAppDarkTheme(): Boolean =
    LocalAppSettingDark.current ?: isSystemInDarkTheme()

/**
 * 解析小部件该用深色还是浅色。
 *
 * 「跟随壁纸」时按**照片本身的明暗**锁死，不跟系统 —— 组件贴在桌面上，
 * 周围是壁纸而不是 App 界面。没壁纸可跟随时退回应用主题。
 */
internal fun resolveWidgetDark(
    wallpaper: ImageBitmap?,
    mode: Int,
    appDark: Boolean,
): Boolean = when (mode) {
    DisplaySettingsPort.THEME_MODE_LIGHT -> false
    DisplaySettingsPort.THEME_MODE_DARK -> true
    DisplaySettingsPort.THEME_MODE_FOLLOW_WALLPAPER ->
        WallpaperStore.measureLightness(wallpaper)?.let { !it } ?: appDark
    else -> appDark
}

/** 卡片是否处于「有壁纸」现场 */
private class WidgetGlassSpec(
    val hasWallpaper: Boolean,
)

private val LocalWidgetGlass = compositionLocalOf<WidgetGlassSpec?> { null }

/** 小部件根节点：给出「有没有壁纸」的现场 */
@Composable
private fun WidgetGlassHost(
    wallpaper: WidgetWallpaper?,
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalWidgetGlass provides WidgetGlassSpec(hasWallpaper = wallpaper != null),
    ) {
        Box(modifier = modifier) {
            Box(modifier = Modifier.matchParentSize()) { background() }
            content()
        }
    }
}

/**
 * 课程卡片的背景。
 * 有壁纸就固定半透明，没壁纸纯色（真机侧也只能是固定值，这里保持一致）。
 *
 * @param tint 额外叠加层（「正在上课」的淡蓝），**不参与**不透明度调节
 */
@Composable
private fun WidgetCardGlass(
    cardColor: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val spec = LocalWidgetGlass.current
    val alpha = if (spec?.hasWallpaper == true) WIDGET_CARD_ALPHA_WITH_WALLPAPER else 1f
    val shape = remember(cornerRadius) { ContinuousRoundedRectangle(cornerRadius) }
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(cardColor.copy(alpha = alpha))
                if (tint != null) drawRect(tint)
            }
    )
}

/**
 * 小部件背景：有壁纸就画壁纸，没壁纸就是原来那块纯色。
 * 壁纸上一律不叠遮罩 —— 可读性交给 [resolveWidgetDark] 决定深浅来解决。
 */
@Composable
private fun WidgetPreviewBackground(
    bgColor: Color,
    wallpaper: WidgetWallpaper?,
    wallpaperBlur: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (wallpaper == null) {
            Box(modifier = Modifier.fillMaxSize().background(bgColor))
        } else {
            WidgetWallpaperLayer(bgColor, wallpaper, wallpaperBlur)
        }
    }
}

/**
 * 一层壁纸。
 * 模糊时**先垫一层主题底色再模糊**：模糊会采样边界外的像素。
 */
@Composable
private fun WidgetWallpaperLayer(
    bgColor: Color,
    wallpaper: WidgetWallpaper,
    blurRadius: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (blurRadius > 0.01f) Modifier.blur(blurRadius.dp) else Modifier)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(bgColor))
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 按容器尺寸现算裁剪区，不拉伸
            val rect = calculateWidgetCropRect(
                bitmapW = wallpaper.bitmap.width,
                bitmapH = wallpaper.bitmap.height,
                containerW = size.width,
                containerH = size.height,
                crop = wallpaper.crop,
            )
            if (rect.width > 0f && rect.height > 0f) {
                drawImage(
                    image = wallpaper.bitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(wallpaper.bitmap.width, wallpaper.bitmap.height),
                    dstOffset = IntOffset(rect.left.roundToInt(), rect.top.roundToInt()),
                    dstSize = IntSize(rect.width.roundToInt(), rect.height.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
            }
        }
    }
}

@Composable
private fun WidgetPreviewToday(
    modifier: Modifier = Modifier,
    wallpaper: WidgetWallpaper? = null,
    appearance: WidgetAppearance = WidgetAppearance(),
    isDark: Boolean = isAppDarkTheme(),
) {
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF7F7F7)
    val cardBg = if (isDark) Color(0xFF262626) else Color.White
    val borderColor = if (isDark) Color(0xFF2A2A2A) else Color(0x0D000000)
    val titleColor = if (isDark) Color(0x77FFFFFF) else Color(0x77000000)
    val nameColor = if (isDark) Color(0xFFEEEEEE) else Color(0xFF1A1A1A)
    val infoColor = if (isDark) Color(0x55FFFFFF) else Color(0x55000000)
    val timeStartColor = if (isDark) Color(0x88FFFFFF) else Color(0x88000000)
    val timeEndColor = if (isDark) Color(0x55FFFFFF) else Color(0x55000000)
    val nowColor = if (isDark) Color(0xFF42A5F5) else Color(0xFF2196F3)
    // 「正在上课」= 卡片底色 + 10% 蓝叠加层
    val activeTint = Color(0x1A2196F3)

    Card(
        cornerRadius = 22.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(WidgetPreviewRatio4x2)
            .border(1.dp, borderColor, ContinuousRoundedRectangle(22.dp)),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = Color.Transparent),
    ) {
        WidgetGlassHost(
            wallpaper = wallpaper,
            modifier = Modifier.fillMaxSize(),
            background = {
                WidgetPreviewBackground(
                    bgColor = bgColor,
                    wallpaper = wallpaper,
                    wallpaperBlur = appearance.wallpaperBlur,
                    modifier = Modifier.fillMaxSize()
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 11.dp, vertical = 9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今天 / 周五",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "第10周",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    )
                }

                CourseCardPreview(
                    "08:00", "09:35", "高等数学", "第1-2节｜A301｜张老师",
                    Color(0xFF4CAF50), null, cardBg, nameColor, infoColor,
                    timeStartColor, timeEndColor, nowColor,
                    Modifier.fillMaxWidth().weight(1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                CourseCardPreview(
                    "14:00", "15:35", "大学英语", "第5-6节｜B205｜李老师",
                    Color(0xFF2196F3), "25分钟结束", cardBg, nameColor, infoColor,
                    timeStartColor, timeEndColor, nowColor,
                    Modifier.fillMaxWidth().weight(1f), tint = activeTint
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun WidgetPreviewSmall2x2(
    modifier: Modifier = Modifier,
    fillBounds: Boolean = false,
    wallpaper: WidgetWallpaper? = null,
    appearance: WidgetAppearance = WidgetAppearance(),
    isDark: Boolean = isAppDarkTheme(),
) {
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color.White
    val borderColor = if (isDark) Color(0xFF2A2A2A) else Color(0x0D000000)
    val titleColor = if (isDark) Color(0x99FFFFFF) else Color(0x99000000)
    val nameColor = if (isDark) Color(0xFFEEEEEE) else Color(0xFF1A1A1A)
    val infoColor = if (isDark) Color(0x88FFFFFF) else Color(0x88000000)
    val remainingColor = if (isDark) Color(0x55FFFFFF) else Color(0x55000000)

    val square: @Composable (Modifier) -> Unit = { squareModifier ->
        WidgetGlassHost(
            wallpaper = wallpaper,
            modifier = squareModifier
                .border(1.dp, borderColor, ContinuousRoundedRectangle(22.dp))
                .clip(ContinuousRoundedRectangle(22.dp)),
            background = {
                WidgetPreviewBackground(
                    bgColor = bgColor,
                    wallpaper = wallpaper,
                    wallpaperBlur = appearance.wallpaperBlur,
                    modifier = Modifier.fillMaxSize()
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, bottom = 4.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "周五", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = titleColor)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(text = "第10周", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = titleColor)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "高等数学",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = nameColor,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_widget_clock),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            alpha = 0.5f
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "08:00 - 09:35",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = infoColor,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.ic_widget_location),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            alpha = 0.5f
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "A301",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = infoColor,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "还剩 2 节课",
                        fontSize = 12.sp,
                        color = remainingColor,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF4CAF50), ContinuousCapsule())
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(Color(0xFF2196F3), ContinuousCapsule())
                    )
                }
            }
        }
    }

    if (fillBounds) {
        // 浮层拿到的矩形就是小部件本体，直接铺满，不再套 4×2 的槽位
        square(modifier.fillMaxSize())
    } else {
        // 外层与 4×2 预览同高，2×2 正方形 fillMaxHeight 居中，保证三页 pager 高度一致。
        // 传入的 modifier（矩形记录 + 让位 alpha）必须挂在正方形上：浮层要的是小部件本体，
        // 不是外面那条 2.15 的槽位，否则 2×2 会被拉成 4×2 的形状。
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(WidgetPreviewRatio4x2),
            contentAlignment = Alignment.Center
        ) {
            square(modifier.fillMaxHeight().aspectRatio(1f))
        }
    }
}

@Composable
private fun WidgetPreviewTodayTomorrow(
    modifier: Modifier = Modifier,
    wallpaper: WidgetWallpaper? = null,
    appearance: WidgetAppearance = WidgetAppearance(),
    isDark: Boolean = isAppDarkTheme(),
) {
    val bgColor = if (isDark) Color(0xFF1A1A1A) else Color(0xFFF7F7F7)
    val cardBg = if (isDark) Color(0xFF262626) else Color.White
    val borderColor = if (isDark) Color(0xFF2A2A2A) else Color(0x0D000000)
    val titleColor = if (isDark) Color(0x77FFFFFF) else Color(0x77000000)
    val nameColor = if (isDark) Color(0xFFEEEEEE) else Color(0xFF1A1A1A)
    val timeColor = if (isDark) Color(0x88FFFFFF) else Color(0x88000000)
    val infoColor = if (isDark) Color(0x55FFFFFF) else Color(0x55000000)

    Card(
        cornerRadius = 22.dp,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(WidgetPreviewRatio4x2)
            .border(1.dp, borderColor, ContinuousRoundedRectangle(22.dp)),
        insideMargin = PaddingValues(0.dp),
        colors = CardDefaults.defaultColors(color = Color.Transparent),
    ) {
        WidgetGlassHost(
            wallpaper = wallpaper,
            modifier = Modifier.fillMaxSize(),
            background = {
                WidgetPreviewBackground(
                    bgColor = bgColor,
                    wallpaper = wallpaper,
                    wallpaperBlur = appearance.wallpaperBlur,
                    modifier = Modifier.fillMaxSize()
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 9.dp, end = 9.dp, top = 9.dp, bottom = 9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 与真实组件一致：两个各占一半的表头槽位，明天槽位左移 4dp 起点
                    Text(
                        text = "今天 / 周五",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "明天 / 周六",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }

                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        TodayTomorrowCardPreview(
                            "高等数学", "08:00-09:35", "A301｜张老师", Color(0xFF4CAF50),
                            cardBg, nameColor, timeColor, infoColor,
                            Modifier.fillMaxWidth().weight(1f)
                        )
                        TodayTomorrowCardPreview(
                            "大学英语", "14:00-15:35", "B205｜李老师", Color(0xFF2196F3),
                            cardBg, nameColor, timeColor, infoColor,
                            Modifier.fillMaxWidth().weight(1f), tint = Color(0x1A2196F3)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        TodayTomorrowCardPreview(
                            "线性代数", "08:00-09:35", "C102｜王老师", Color(0xFFFF9800),
                            cardBg, nameColor, timeColor, infoColor,
                            Modifier.fillMaxWidth().weight(1f)
                        )
                        TodayTomorrowCardPreview(
                            "大学物理", "10:00-11:35", "D203｜赵老师", Color(0xFF9C27B0),
                            cardBg, nameColor, timeColor, infoColor,
                            Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayTomorrowCardPreview(
    name: String, time: String, info: String,
    barColor: Color, cardBg: Color,
    nameColor: Color, timeColor: Color, infoColor: Color,
    modifier: Modifier = Modifier,
    /** 额外叠加层（正在上课的淡蓝），不受卡片不透明度影响 */
    tint: Color? = null,
) {
    Card(
        cornerRadius = 14.dp,
        modifier = modifier,
        // 底色交给 WidgetCardGlass
        colors = CardDefaults.defaultColors(
            color = Color.Transparent,
            contentColor = Color.Transparent
        ),
        insideMargin = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WidgetCardGlass(
                cardColor = cardBg,
                cornerRadius = 14.dp,
                tint = tint,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 6.dp)
                        .width(4.dp)
                        .background(barColor, ContinuousRoundedRectangle(2.dp))
                )
                Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = nameColor, maxLines = 1)
                    Text(time, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = timeColor, maxLines = 1)
                    Text(info, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = infoColor, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CourseCardPreview(
    startTime: String, endTime: String, name: String, info: String,
    barColor: Color, remaining: String?,
    cardBg: Color, nameColor: Color, infoColor: Color,
    timeStartColor: Color, timeEndColor: Color, nowColor: Color,
    modifier: Modifier = Modifier,
    /** 额外叠加层（正在上课的淡蓝），不受卡片不透明度影响 */
    tint: Color? = null,
) {
    Card(
        cornerRadius = 12.dp,
        modifier = modifier,
        colors = CardDefaults.defaultColors(
            color = Color.Transparent,
            contentColor = Color.Transparent
        ),
        insideMargin = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            WidgetCardGlass(
                cardColor = cardBg,
                cornerRadius = 12.dp,
                tint = tint,
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.width(35.dp), horizontalAlignment = Alignment.Start) {
                    Text(startTime, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = timeStartColor)
                    Text(endTime, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = timeEndColor)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .background(barColor, ContinuousRoundedRectangle(2.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = nameColor, maxLines = 1)
                    Text(info, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = infoColor)
                }
                if (remaining != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        remaining,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = nowColor,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(title: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(top = 7.dp)
                .background(MiuixTheme.colorScheme.primary, ContinuousRoundedRectangle(4.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MiuixTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceVariantActions)
        }
    }
}

@Composable
private fun WidgetPaddingSelector(
    selectedMode: Int,
    onSelect: (Int) -> Unit
) {
    val entry = DropdownEntry(
        items = listOf(
            DropdownItem(
                text = "标准",
                selected = selectedMode == 0,
                onClick = { onSelect(0) }
            ),
            DropdownItem(
                text = "4×6",
                selected = selectedMode == 1,
                onClick = { onSelect(1) }
            ),
            DropdownItem(
                text = "4×7",
                selected = selectedMode == 2,
                onClick = { onSelect(2) }
            ),
            DropdownItem(
                text = "4×6 无字",
                selected = selectedMode == 3,
                onClick = { onSelect(3) }
            ),
            DropdownItem(
                text = "4×7 无字",
                selected = selectedMode == 4,
                onClick = { onSelect(4) }
            ),
        )
    )
    val liquidGlassDropdownColors = DropdownDefaults.dropdownColors(
        containerColor = Color.Transparent,
        selectedContainerColor = Color.Transparent,
    )
    AppCard(modifier = Modifier.fillMaxWidth()) {
        GlassDropdownMenu(
            title = "小组件适应布局",
            summary = "按桌面留白选择上下内缩档位",
            entry = entry,
            collapseOnSelection = true,
            liquidGlassBackdrop = LocalLiquidGlassBackdrop.current,
            dropdownColors = liquidGlassDropdownColors,
        )
    }
}
