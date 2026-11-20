/** 自定义模糊底部弹窗 - 支持全区域模糊背景 */
package top.yukonga.miuix.kmp.overlay

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.backdrops.layerBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberCanvasBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberLayerBackdrop
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.backdrop.effects.runtimeShaderEffect
import com.haooz.softglass.platform.isRenderEffectSupported
import com.haooz.softglass.shape.ContinuousRoundedRectangle
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.anim.folmeSpring
import top.yukonga.miuix.kmp.layout.TopBarMaterialHideSpec
import top.yukonga.miuix.kmp.layout.TopBarMaterialShowSpec
import top.yukonga.miuix.kmp.layout.rememberCollapsibleTopAppBarState
import top.yukonga.miuix.kmp.layout.rememberSharedScrollBehavior
import top.yukonga.miuix.kmp.material.LocalGlassDialogBlurDp
import top.yukonga.miuix.kmp.material.LocalMaterialNoneEnabled
import top.yukonga.miuix.kmp.material.LocalSoftGlassLayer
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalForcedDarkTheme
import top.yukonga.miuix.kmp.theme.LocalMonetColorEnabled
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.rememberAppSettingDark
import top.yukonga.miuix.kmp.utils.LocalOverScrollState
import top.yukonga.miuix.kmp.utils.LocalPredictiveBackEnabled
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.DialogLayout
import top.yukonga.miuix.kmp.utils.OverScrollState
import top.yukonga.miuix.kmp.utils.rememberNavigationBack

/** 弹窗面板玻璃的 shader 缓存键。 */
private const val SOFT_GLASS_SHEET_KEY = "MiuixSoftGlassBottomSheet"

/**
 * 顶栏材质动画值载体：由 BlurBottomSheet 顶栏机制下发，供 startAction/endAction
 * 内部的 LiquidTopBarButton 读取，驱动液态玻璃材质/阴影随滚动渐变。
 * 持有 Animatable 而非快照值，使 provider 下发稳定实例，材质动画期间不会逐帧重组整个弹窗内容。
 */
@Stable
class SheetTopBarMaterial internal constructor(
    internal val backdropAlphaAnimatable: Animatable<Float, AnimationVector1D>,
    internal val shadowAlphaAnimatable: Animatable<Float, AnimationVector1D>,
) {
    val backdropAlpha: Float get() = backdropAlphaAnimatable.value
    val shadowAlpha: Float get() = shadowAlphaAnimatable.value
}

val LocalSheetTopBarMaterial = compositionLocalOf {
    SheetTopBarMaterial(Animatable(1f), Animatable(1f))
}

/**
 * 弹窗内容自身的 backdrop（内部 rememberLayerBackdrop 捕获）。
 * 玻璃组件应从此读取，而非提升为屏幕级 State：回写外层 State 会让页面在进入动画最吃紧时重组，是掉帧主因；
 * CompositionLocal 下发不触碰外层作用域，零重组成本。
 */
val LocalSheetContentBackdrop = compositionLocalOf<GlassBackdrop?> { null }

/**
 * 非快照 backdrop 持有者，配合 onSheetContentBackdropCreated 使用，
 * 供弹窗外部的二级 OverlayDialog（如节次/时间选择器）采样弹窗内容做模糊——它们读不到 LocalSheetContentBackdrop。
 * 用普通对象而非 State：写 State 会让宿主页面在挂载后重组（掉帧主因），且二级弹窗交互后才组合、backdrop 早已就绪。
 */
class BackdropHolder {
    var value: GlassBackdrop? = null
}

/**
 * 自定义模糊底部弹窗组件，支持全区域（包括标题栏）的模糊背景效果。
 *
 * 只有一种形态：打开时高度吸附内容（半屏），拖顶部把手可拉满全屏，下拉关闭。
 * 不再区分「可变高度 / 固定高度」两套实现，所有弹窗共用同一套手势与高度计算。
 *
 * @param show 是否显示
 * @param title 标题文字
 * @param dimBackground 是否压暗背景
 * @param sheetBackgroundColor 弹窗背景颜色，null 则使用默认颜色
 * @param onDismissRequest 关闭回调
 * @param startAction 标题栏左侧操作按钮
 * @param endAction 标题栏右侧操作按钮
 * @param onSheetContentBackdropCreated 弹窗内容 backdrop 创建回调
 * @param headerOverlay 叠在弹窗外壳顶部的悬浮层（渐变模糊顶栏、浮动分段标签等）。
 * 渲染在被 sheetContentBackdrop 录制的内容层之外，可安全采样 [LocalSheetContentBackdrop]。
 * @param content 内容区域
 */
@Composable
fun BlurBottomSheet(
    show: Boolean,
    title: String,
    dimBackground: Boolean = false,
    sheetBackgroundColor: Color? = null,
    sheetMaxWidth: Dp = Dp.Unspecified,
    /**
     * 弹窗本体的玻璃采样源；null = 保持实色（默认）。
     *
     * 传入后弹窗面板会走柔光玻璃（模糊 + 折射 + 边光），底色降为半透明以保证文字可读。
     * 采样源传**页面内容录制层**（`LocalLiquidGlassBackdrop`）：弹窗盖在页面上，
     * 玻璃透出的就该是它下面那一页；弹窗本身画在录制层之外，不会采到自己。
     * 传纯壁纸层会丢掉页面内容，看起来像悬空的磨砂板。
     */
    glassBackdrop: GlassBackdrop? = null,
    onDismissRequest: () -> Unit,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    onSheetContentBackdropCreated: ((GlassBackdrop?) -> Unit)? = null,
    headerOverlay: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val visibleState = remember { mutableStateOf(show) }
    // 显示时立即可见，隐藏时等动画播完再隐藏
    LaunchedEffect(show) {
        if (show) {
            visibleState.value = true
        }
    }
    // 回调直接由内容层触发，不再经 State 中转：State 在组合期被读取，挂载后写入会让本作用域在打开动画头几帧重组一次。

    DialogLayout(
        visible = visibleState,
        enableWindowDim = false,
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        enableAutoLargeScreen = false,
        renderInRootScaffold = true,
    ) {
        BlurBottomSheetContent(
            show = show,
            visibleState = visibleState,
            title = title,
            dimBackground = dimBackground,
            sheetBackgroundColor = sheetBackgroundColor,
            glassBackdrop = glassBackdrop,
            onDismissRequest = onDismissRequest,
            startAction = startAction,
            endAction = endAction,
            onSheetContentBackdropCreated = onSheetContentBackdropCreated,
            headerOverlay = headerOverlay,
            sheetMaxWidth = sheetMaxWidth,
            content = content,
        )
    }
}

@Composable
private fun BlurBottomSheetContent(
    show: Boolean,
    visibleState: MutableState<Boolean>,
    title: String,
    dimBackground: Boolean = false,
    sheetBackgroundColor: Color? = null,
    sheetMaxWidth: Dp = Dp.Unspecified,
    glassBackdrop: GlassBackdrop? = null,
    onDismissRequest: () -> Unit,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    onSheetContentBackdropCreated: ((GlassBackdrop?) -> Unit)? = null,
    headerOverlay: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // 弹窗跟随应用主题，不受壁纸强制主题影响。
    // ⚠ 取色开关必须一并带上：自建 controller 会改写 LocalColorSchemeMode 为自己的 mode，
    // 只给 Light/Dark 会让弹窗内 isDynamicColor 恒为 false，莫奈取色分支全部变死代码。
    val sheetAppDark = rememberAppSettingDark()
    val sheetMonetColor = LocalMonetColorEnabled.current
    val sheetAppController = remember(sheetAppDark, sheetMonetColor) {
        ThemeController(
            when {
                sheetMonetColor && sheetAppDark -> ColorSchemeMode.MonetDark
                sheetMonetColor -> ColorSchemeMode.MonetLight
                sheetAppDark -> ColorSchemeMode.Dark
                else -> ColorSchemeMode.Light
            }
        )
    }
    CompositionLocalProvider(LocalForcedDarkTheme provides null) {
        MiuixTheme(controller = sheetAppController) {
            val animationProgress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val sheetHeightPx = remember { mutableIntStateOf(0) }
    val imeInsets = WindowInsets.ime
    // 预测性返回：返回手势进度实时驱动 sheet 下滑（API 33+），取消回弹、完成关闭
    val backState = rememberNavigationBack()
    val backProgress = remember { Animatable(0f) }
    val detentState = remember { BlurBottomSheetDetentState() }
    val isDetentExpanded by remember(detentState) {
        derivedStateOf { detentState.expansionFraction > 0.001f }
    }

    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    // sheet 底色：莫奈取色跟随 surfaceContainer；非莫奈保持硬编码（暗 #1E1E1E / 亮 #F2F2F2）。
    val sheetBgColor = sheetBackgroundColor ?: if (MiuixTheme.isDynamicColor) {
        MiuixTheme.colorScheme.surfaceContainer
    } else if (isDark) Color(0xFF1E1E1E) else Color(0xFFF2F2F2)
    val dismissThresholdPx = with(density) { 150.dp.toPx() }
    val velocityThresholdPx = with(density) { 800.dp.toPx() }

    // 圆角固定 36dp：不跟随屏幕物理圆角，否则圆角大的机型会过大。
    val sheetShape = remember { ContinuousRoundedRectangle(36.dp) }
    val windowWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val horizontalGap = if (sheetMaxWidth != Dp.Unspecified) {
        ((windowWidth - minOf(sheetMaxWidth, windowWidth)) / 2).coerceAtLeast(0.dp)
    } else 0.dp

    // ── 弹窗玻璃 ────────────────────────────────────────────────────────────────
    // 传了 glassBackdrop 且柔光材质可用时，整块面板走柔光玻璃；底色降为半透明保证文字可读。
    // 没传（自定义 / 效果这类内部面板）保持原来的实色，观感不变。
    val softGlass = LocalSoftGlassLayer.current
    val dialogBlur = LocalGlassDialogBlurDp.current
    val glassActive = glassBackdrop != null && softGlass != null && isRenderEffectSupported()
    val sheetCornerPx = with(density) { 36.dp.toPx() }
    val glassShapeBlock: () -> Shape = remember { { sheetShape } }
    val glassEffects: GlassEffectScope.() -> Unit =
        remember(softGlass, dialogBlur, sheetCornerPx) {
            {
                val layer = softGlass
                if (layer != null) {
                    blur(dialogBlur.dp.toPx(), TileMode.Mirror)
                    padding = 0f
                    runtimeShaderEffect(
                        key = SOFT_GLASS_SHEET_KEY,
                        shaderString = layer.shaderString,
                        uniformShaderName = "content",
                    ) {
                        layer.applyDialogUniforms(this, size.width, size.height, sheetCornerPx)
                    }
                }
            }
        }
    // 玻璃档的面色：柔光 shader 自己出一层玻璃本色，这里只补一点不透明度让正文可读
    val sheetBodyColor =
        if (glassActive) {
            sheetBgColor.copy(alpha = if (isDark) 0.52f else 0.42f)
        } else {
            sheetBgColor
        }

            // 显示/隐藏动画同时驱动位移与遮罩透明度，确保二者完全同步
    LaunchedEffect(show) {
        if (show) {
            detentState.reset()
            backProgress.snapTo(0f)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 480,
                    easing = CubicBezierEasing(0.34f, 1.12f, 0.3f, 1f)
                )
            )
        } else {
            animationProgress.animateTo(0f, animationSpec = tween(320, easing = CubicBezierEasing(0.34f, 1f, 0.3f, 1f)))
            visibleState.value = false
        }
    }

    // 底部弹窗主体 - 允许内容溢出屏幕底部
    val dimModifier = if (dimBackground) {
        Modifier.drawBehind {
            drawRect(Color.Black.copy(alpha = 0.2f * animationProgress.value * (1f - backProgress.value)))
        }
    } else Modifier

    // onDismiss 身份不稳定，不能作为 pointerInput key，否则每次重组都会重启手势协程。
    val currentShow by rememberUpdatedState(show)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    val coroutineScope = rememberCoroutineScope()

    // 返回手势：进行中驱动 sheet 下滑，取消回弹、完成关闭。
    backState.handler(
        enabled = show,
        onBackCancelled = {
            coroutineScope.launch {
                backProgress.animateTo(0f, animationSpec = tween(150))
            }
        },
        onBackCompleted = {
            currentOnDismissRequest()
            coroutineScope.launch {
                // 若调用方拦截了返回事件（例如子页面退回上级）而未关闭弹窗，需复位滑动偏移
                withFrameNanos { }
                if (currentShow) {
                    backProgress.animateTo(0f, animationSpec = tween(150))
                }
            }
        },
    )

    // 逐帧收集返回手势进度（单独协程，避免手势期间每帧取消/重启 LaunchedEffect）；
    // LocalPredictiveBackEnabled 须在组合期读取（协程内 .current 编译不过）。
    val predictiveBackEnabled = LocalPredictiveBackEnabled.current

    LaunchedEffect(Unit) {
        // 预测性返回关闭时连进度都不订阅（返回仍被拦截，直接关闭）；
        // backProgress() 非手势期间返回 null（保持静止），手势进行中为连续进度。
        if (!predictiveBackEnabled) return@LaunchedEffect
        snapshotFlow { backState.backProgress() }
            .collect { progress ->
                if (progress != null) {
                    backProgress.snapTo(progress)
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(dimModifier)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { currentOnDismissRequest() })
            },
    ) {
        // 窗口高度在组合期读取一次，避免进入/拖拽动画期间逐帧做密度换算。
        val windowHeightPx = with(density) { windowInfo.containerSize.height.toFloat() }
        val sheetModifier = Modifier
            .graphicsLayer {
                val progress = animationProgress.value
                val currentHeight = sheetHeightPx.intValue.toFloat()
                val baseOffset = if (currentHeight > 0) currentHeight else windowHeightPx
                // backProgress：返回手势把 sheet 向下推出屏幕；dismissOffsetPx：把手下拉的收起位移
                translationY = baseOffset * (1f - progress) +
                    backProgress.value * currentHeight +
                    detentState.dismissOffsetPx
            }
            // 水平系统栏避让加在外壳上：整张卡片（底色/圆角/边缘光）一起内缩，内容不再单独避让；
            // 否则会出现「实底铺到系统栏下、文字却缩一条」的歪斜。指定 sheetMaxWidth 时为居中窄条不贴边，不加。
            .then(
                if (sheetMaxWidth == Dp.Unspecified) {
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                } else Modifier
            )

        // 卡片整体下移：全屏态时顶上沿落在状态栏下方（顶栏标题才不会钻到状态栏里），半屏态靠这段位移把底边溢出屏幕。
        val sheetOffsetDpValue = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 5.dp
        val sheetDirtyUntil = remember { arrayOfNulls<TimeMark>(1) }
        val markSheetDirty: (Long) -> Unit = remember(sheetDirtyUntil) {
            { durationMs ->
                sheetDirtyUntil[0] = TimeSource.Monotonic.markNow() + durationMs.milliseconds
            }
        }

        Box(
            modifier = sheetModifier
                .offset(y = sheetOffsetDpValue)
                .align(Alignment.BottomCenter)
                .then(
                    if (sheetMaxWidth != Dp.Unspecified) Modifier.width(sheetMaxWidth).fillMaxWidth()
                    else Modifier.fillMaxWidth()
                )
                .heightIn(max = with(density) { windowInfo.containerSize.height.toDp() })
                .blurBottomSheetDetentHeight(detentState)
                .onGloballyPositioned { coordinates ->
                    if (imeInsets.getBottom(density) == 0) {
                        val newHeight = coordinates.size.height
                        if (sheetHeightPx.intValue != newHeight) {
                            sheetHeightPx.intValue = newHeight
                        }
                        detentState.updateCollapsedHeight(newHeight)
                    }
                }
                .imePadding()
                .clip(sheetShape)
                // 传了 glassBackdrop 才挂柔光玻璃；否则保持原来的纯实色
                .then(
                    if (glassActive) {
                        Modifier.drawBackdrop(
                            // glassActive 已经蕴含非空
                            backdrop = glassBackdrop!!,
                            shape = glassShapeBlock,
                            effects = glassEffects,
                            shadow = null,
                        )
                    } else {
                        Modifier
                    }
                )
                .background(sheetBodyColor)
                // 上下方向高光描边：纯 Compose 画，不走 AGSL
                .sheetRimHighlight(isDark, 36.dp)
                .consumeWindowInsets(PaddingValues(horizontal = horizontalGap))
                .pointerInput(markSheetDirty) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            markSheetDirty(550L)
                        }
                    }
                }
                .pointerInput(Unit) {
                    // 消费空白处点击，防止事件穿透到背景层触发关闭
                    detectTapGestures(onTap = {})
                }
                .semantics {
                    onClick(label = "Dismiss") {
                        onDismissRequest()
                        true
                    }
                },
            content = {
                // 顶栏机制
                val topBarState = rememberCollapsibleTopAppBarState()
                val scrollBehavior = rememberSharedScrollBehavior(topBarState)
                val overScrollState = remember { OverScrollState() }
                LaunchedEffect(Unit) { topBarState.heightOffsetLimit = -1f }

                val showButtonShadow by remember(scrollBehavior) {
                    derivedStateOf {
                        val contentOffset = scrollBehavior.state.contentOffset
                        val os = overScrollState.offset
                        contentOffset < 0f || os < 0f
                    }
                }
                // overscroll 时 OverScrollNode.onPostScroll 把 available.y 全返父级，被 scrollBehavior 累加进 contentOffset
                // （松手不归零，阴影错保持）。代理连接在 overscroll 激活时把 consumed.y 置零，contentOffset 不再被污染；
                // 零滚动页面 co 恒为 0，可滚动页面仅累积真实滚动量。
                val proxyConnection = remember(scrollBehavior, overScrollState, markSheetDirty) {
                    val delegate = scrollBehavior.nestedScrollConnection
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (available != Offset.Zero) markSheetDirty(450L)
                            return delegate.onPreScroll(available, source)
                        }

                        override fun onPostScroll(
                            consumed: Offset,
                            available: Offset,
                            source: NestedScrollSource,
                        ): Offset {
                            if (consumed != Offset.Zero || available != Offset.Zero) markSheetDirty(450L)
                            // 用 offset != 0f 判断而非 isOverScrollActive：后者在 0~offsetThreshold(1f) 间仍为 false，会漏判几帧污染
                            if (overScrollState.offset != 0f) {
                                return delegate.onPostScroll(Offset.Zero, available, source)
                            }
                            return delegate.onPostScroll(consumed, available, source)
                        }

                        override suspend fun onPreFling(available: Velocity): Velocity {
                            if (available != Velocity.Zero) markSheetDirty(650L)
                            return delegate.onPreFling(available)
                        }

                        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                            markSheetDirty(650L)
                            return delegate.onPostFling(consumed, available)
                        }
                    }
                }
                val shadowAlpha = remember { Animatable(0f) }
                val backdropAlpha = remember { Animatable(0f) }
                // 稳定实例，provider 不逐帧下发新对象
                val topBarMaterial = remember { SheetTopBarMaterial(backdropAlpha, shadowAlpha) }
                LaunchedEffect(showButtonShadow) {
                    val target = if (showButtonShadow) 1f else 0f
                    // 弹簧唯一来源：miuix layout/TopBarMaterial.kt（与全站顶栏同一套手感）
                    val spec = if (showButtonShadow) TopBarMaterialShowSpec else TopBarMaterialHideSpec
                    launch { shadowAlpha.animateTo(target, spec) }
                    launch { backdropAlpha.animateTo(target, spec) }
                }

                // 录制时先铺面色（只进 backdrop，不改屏幕合成），使渐进模糊底边与外壳颜色对齐
                val sheetContentBackdrop = rememberLayerBackdrop(
                    onDraw = remember(sheetBodyColor) {
                        {
                            drawRect(sheetBodyColor)
                            drawContent()
                        }
                    }
                )
                LaunchedEffect(sheetContentBackdrop) {
                    onSheetContentBackdropCreated?.invoke(sheetContentBackdrop)
                }

                // 进入动画早期挂载 backdrop/AGSL：原先等 enterDone 再挂，卡顿落在「动画刚结束、用户要点遮罩」的窗口，
                // 表现为遮罩暂时点不动而返回键可用。
                // 无材质档：内容录屏层不挂（采样侧退回纯色 placeholder）
                val materialNone = LocalMaterialNoneEnabled.current
                var sheetBackdropMounted by remember { mutableStateOf(false) }
                LaunchedEffect(show, materialNone) {
                    if (show && !materialNone) {
                        delay(80.milliseconds)
                        sheetBackdropMounted = true
                    }
                }

                val placeholderOnDraw: DrawScope.() -> Unit = remember(sheetBodyColor) {
                    {
                        drawRect(
                            color = sheetBodyColor,
                            topLeft = Offset(-size.width, -size.height),
                            size = Size(size.width * 3f, size.height * 3f)
                        )
                    }
                }
                val placeholderBackdrop = rememberCanvasBackdrop(placeholderOnDraw)

                CompositionLocalProvider(
                    LocalOverScrollState provides overScrollState,
                    LocalSheetTopBarMaterial provides topBarMaterial,
                    LocalSheetContentBackdrop provides
                            if (sheetBackdropMounted) sheetContentBackdrop else placeholderBackdrop,
                    LocalBlurBottomSheetContentExpanded provides isDetentExpanded,
                ) {
                    BlurBottomSheetDragHandle(
                        onDrag = detentState::dragBy,
                        onDragStarted = { detentState.beginDrag() },
                        onDragStopped = { velocity ->
                            detentState.settle(
                                velocityY = velocity,
                                velocityThresholdPx = velocityThresholdPx,
                                dismissThresholdPx = dismissThresholdPx,
                                onDismiss = currentOnDismissRequest,
                            )
                        },
                    )

                    val sheetRecordKey = remember(title, sheetBodyColor, isDark, isDetentExpanded, sheetBackdropMounted) {
                        listOf(title, sheetBodyColor, isDark, isDetentExpanded, sheetBackdropMounted)
                    }
                    val sheetMustRecord: () -> Boolean = remember(
                        scrollBehavior,
                        overScrollState,
                        detentState,
                        animationProgress,
                        backProgress,
                        shadowAlpha,
                        backdropAlpha,
                        sheetDirtyUntil,
                    ) {
                        var lastContentOffset = Float.NaN
                        var lastOverScroll = Float.NaN
                        var lastExpansion = Float.NaN
                        var lastDismissOffset = Float.NaN
                        var lastShadowAlpha = Float.NaN
                        var lastAnimProgress = Float.NaN
                        var lastBackProgress = Float.NaN
                        {
                            val contentOffset = scrollBehavior.state.contentOffset
                            val overScroll = overScrollState.offset
                            val expansion = detentState.expansionFraction
                            val dismissOffset = detentState.dismissOffsetPx
                            val shadow = shadowAlpha.value
                            val anim = animationProgress.value
                            val back = backProgress.value
                            val moved =
                                contentOffset != lastContentOffset ||
                                    overScroll != lastOverScroll ||
                                    expansion != lastExpansion ||
                                    dismissOffset != lastDismissOffset ||
                                    shadow != lastShadowAlpha ||
                                    anim != lastAnimProgress ||
                                    back != lastBackProgress
                            lastContentOffset = contentOffset
                            lastOverScroll = overScroll
                            lastExpansion = expansion
                            lastDismissOffset = dismissOffset
                            lastShadowAlpha = shadow
                            lastAnimProgress = anim
                            lastBackProgress = back
                            moved ||
                                animationProgress.isRunning ||
                                backProgress.isRunning ||
                                shadowAlpha.isRunning ||
                                backdropAlpha.isRunning ||
                                (sheetDirtyUntil[0]?.hasPassedNow() == false)
                        }
                    }

                    // 内容区域（layerBackdrop 捕获「底色+内容」）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isDetentExpanded) {
                                    Modifier.fillMaxHeight()
                                } else {
                                    Modifier.wrapContentHeight()
                                },
                            )
                            .nestedScroll(proxyConnection)
                            .sheetTopOpacityMask(84.dp) { shadowAlpha.value }
                            .then(
                                if (sheetBackdropMounted) {
                                    Modifier.layerBackdrop(
                                        backdrop = sheetContentBackdrop,
                                        recordKey = sheetRecordKey,
                                        mustRecord = sheetMustRecord,
                                    )
                                } else Modifier
                            )
                            // 水平避让已提到外壳，此处不再内缩，否则双倍避让
                    ) {
                        content()
                    }

                    // 顶部渐变遮罩：顶部铺满弹窗面色、向下淡出到透明
                    // （玻璃档的面色本身是半透明，所以渐变也按同一份 alpha 收）
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .zIndex(1f)
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to sheetBodyColor,
                                        0.2f to sheetBodyColor.copy(alpha = sheetBodyColor.alpha * 0.95f),
                                        0.5f to sheetBodyColor.copy(alpha = sheetBodyColor.alpha * 0.8f),
                                        0.8f to sheetBodyColor.copy(alpha = sheetBodyColor.alpha * 0.45f),
                                        1.0f to sheetBodyColor.copy(alpha = 0f),
                                    ),
                                )
                            )
                    )

                    // 标题栏（zIndex 置顶消费触摸）；固定高度使有无操作按钮时标题都垂直居中
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            // 水平避让已在外壳，标题按卡片实际宽度居中
                            .zIndex(2f),
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = MiuixTheme.textStyles.title4.fontSize,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        if (startAction != null) {
                            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                                startAction()
                            }
                        }
                        if (endAction != null) {
                            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                endAction()
                            }
                        }
                    }

                    // 悬浮头部层：在内容录制层之外，可安全采样 LocalSheetContentBackdrop。
                    // z 序夹在顶部渐变与标题带之间，滚动时模糊带盖住渐变，标题与按钮仍在最上层。
                    if (headerOverlay != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1.5f),
                        ) {
                            headerOverlay()
                        }
                    }
                }
            },
        )
        }
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
