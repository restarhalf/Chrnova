package restarhalf.stellar.schedule.ui.components.bottombar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.backdrops.layerBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberCombinedBackdrop
import com.haooz.softglass.backdrop.backdrops.rememberLayerBackdrop
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.backdrop.effects.runtimeShaderEffect
import com.haooz.softglass.platform.GlassRuntimeShader
import com.haooz.softglass.platform.isRenderEffectSupported
import com.haooz.softglass.platform.isRuntimeShaderSupported
import com.haooz.softglass.shape.ContinuousCapsule
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.tanh
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.ui.components.animation.DampedDragAnimation
import restarhalf.stellar.schedule.ui.components.animation.InteractiveHighlight
import restarhalf.stellar.schedule.ui.modifier.InnerShadow
import restarhalf.stellar.schedule.ui.modifier.innerShadow
import restarhalf.stellar.schedule.ui.modifier.lens
import restarhalf.stellar.schedule.ui.modifier.softGlassRim
import restarhalf.stellar.schedule.ui.modifier.vibrancy
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalLiquidBottomTabScale =
    staticCompositionLocalOf { { 1f } }

internal class LiquidBottomBarMorphState(
    val collapseProgress: () -> Float,
    val jellyOvershoot: () -> Float,
    val morphMode: () -> LeftMorphMode,
    val selectedTabIndex: () -> Int,
    val tabsCount: Int,
    val tabWidthPx: Float,
    val padPx: Float,
    val orbDiameterPx: Float,
    val pillWidthPx: Float,
    val isLtr: Boolean,
)

internal val LocalLiquidBottomBarMorphState =
    staticCompositionLocalOf<LiquidBottomBarMorphState?> { null }

private val DROP_UNIFORM_NAMES = arrayOf(
    "drop0", "drop1", "drop2", "drop3",
    "drop4", "drop5", "drop6", "drop7",
)

private fun computeTabStartCenterX(
    morph: LiquidBottomBarMorphState,
    index: Int,
): Float {
    val count = morph.tabsCount.coerceAtLeast(1)
    return if (morph.isLtr) {
        morph.padPx + (index + 0.5f) * morph.tabWidthPx
    } else {
        morph.padPx + (count - 0.5f - index) * morph.tabWidthPx
    }
}

private fun computeStreamRevealT(
    morph: LiquidBottomBarMorphState,
    startCenterX: Float,
    p: Float,
): Float {
    val frontX = lerp(morph.orbDiameterPx, morph.pillWidthPx, computeExpandFrontT(p))
    val effectiveFrontX = lerp(0f, frontX, morphSmoothstep(0.06f, 0.22f, p))
    val revealEnd = minOf(morph.pillWidthPx - morph.padPx, startCenterX + morph.tabWidthPx * 0.45f)
    val revealStart = (revealEnd - morph.tabWidthPx * 0.75f).coerceAtLeast(morph.padPx * 0.5f)
    return morphSmoothstep(revealStart, revealEnd, effectiveFrontX)
}

private fun selectorScaleMultipliers(
    p: Float,
    mode: LeftMorphMode,
): Pair<Float, Float> {
    val c = 1f - p
    return if (mode == LeftMorphMode.CollapseBurst) {
        val shrinkT = (c / 0.20f).coerceIn(0f, 1f)
        lerp(1f, 0.65f, shrinkT) to lerp(1f, 0.78f, shrinkT)
    } else {
        val appearT = morphSmoothstep(0.60f, 0.90f, p)
        lerp(0.76f, 1f, appearT) to lerp(0.86f, 1f, appearT)
    }
}

data class LiquidBottomTrailing(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val gap: Dp = 10.dp,
    val dragRoom: Dp = 18.dp,
    val iconTint: Color? = null,
)

@Composable
fun RowScope.LiquidBottomTab(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidBottomTabScale.current
    // 必须在平移之前不做单槽 clip，否则被选中的非首项滑向左侧球时会被原槽位裁剪消失
    Column(
        modifier
            .semantics { role = Role.Tab }
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/**
 * 液态底栏导航项：支持展开态正常排列，以及非对称液态收起/展开联动：
 * - 收起（[LeftMorphMode.CollapseBurst]）：胶囊裂解成水滴向左聚集时，选中项平滑滑入左球居中，
 *   未选中项随碎滴微散并快速淡出；
 * - 展开（[LeftMorphMode.ExpandStream]）：左侧玻璃球蓄力甩出 3 颗递减液态水珠拉丝吸合融成胶囊，
 *   选中项随液态势能平滑复位，未选中项与文字按吸合推进顺序依次浮现。
 */
@Composable
fun RowScope.LiquidBottomTab(
    index: Int,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = LocalLiquidBottomTabScale.current
    val morph = LocalLiquidBottomBarMorphState.current
    val density = LocalDensity.current
    val iconGapPx = with(density) { 2f.dp.toPx() }
    val scatterOffsetPx = with(density) { 6f.dp.toPx() }
    val streamSlideInPx = with(density) { 10f.dp.toPx() }
    var labelHeightPx by remember { mutableIntStateOf(0) }

    // 必须在平移之前不做单槽 clip，否则被选中的非首项滑向左侧球时会被原槽位裁剪消失
    Column(
        modifier
            .semantics { role = Role.Tab }
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val baseScale = scale()
                if (morph == null) {
                    scaleX = baseScale
                    scaleY = baseScale
                    translationX = 0f
                    translationY = 0f
                    alpha = 1f
                    return@graphicsLayer
                }

                val p = morph.collapseProgress().coerceIn(0f, 1f)
                val c = 1f - p
                if (c <= 0.001f) {
                    scaleX = baseScale
                    scaleY = baseScale
                    translationX = 0f
                    translationY = 0f
                    alpha = 1f
                    return@graphicsLayer
                }

                val startCenterX = computeTabStartCenterX(morph, index)
                val orbCenterX = morph.orbDiameterPx * 0.5f
                val isSelected = morph.selectedTabIndex() == index
                val mode = morph.morphMode()

                if (mode == LeftMorphMode.CollapseBurst) {
                    if (isSelected) {
                        val glideT = FastOutSlowInEasing.transform(((c - 0.04f) / 0.58f).coerceIn(0f, 1f))
                        translationX = lerp(startCenterX, orbCenterX, glideT) - startCenterX
                        translationY = 0f
                        val burstOrbScale = if (c < 0.35f) {
                            lerp(1.0f, 0.72f, morphSmoothstep(0.00f, 0.35f, c))
                        } else {
                            lerp(0.72f, 1.0f, morphSmoothstep(0.35f, 0.92f, c))
                        }
                        val jellyPulse = 1f + morph.jellyOvershoot() * 0.06f
                        val s = baseScale * burstOrbScale * jellyPulse
                        scaleX = s
                        scaleY = s
                        alpha = 1f
                    } else {
                        val scatterT = morphSmoothstep(0f, 0.24f, c)
                        val signY = if (index % 2 == 0) -1f else 1f
                        translationX = 0f
                        translationY = scatterOffsetPx * signY * scatterT
                        val s = baseScale * lerp(1f, 0.55f, scatterT)
                        scaleX = s
                        scaleY = s
                        alpha = (1f - scatterT).coerceIn(0f, 1f)
                    }
                } else {
                    if (isSelected) {
                        val glideT = FastOutSlowInEasing.transform(((p - 0.10f) / 0.64f).coerceIn(0f, 1f))
                        val currentX = lerp(orbCenterX, startCenterX, glideT)
                        translationX = currentX - startCenterX
                        translationY = 0f
                        val jellyPulse = 1f + morph.jellyOvershoot() * 0.06f
                        val s = baseScale * jellyPulse
                        scaleX = s
                        scaleY = s
                        alpha = 1f
                    } else {
                        val revealT = computeStreamRevealT(morph, startCenterX, p)
                        translationX = lerp(-streamSlideInPx, 0f, revealT)
                        translationY = 0f
                        val s = baseScale * lerp(0.80f, 1f, revealT)
                        scaleX = s
                        scaleY = s
                        alpha = revealT
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(2f.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                if (morph == null) {
                    translationY = 0f
                    return@graphicsLayer
                }
                val p = morph.collapseProgress().coerceIn(0f, 1f)
                val c = 1f - p
                if (c <= 0.001f || morph.selectedTabIndex() != index) {
                    translationY = 0f
                    return@graphicsLayer
                }
                val centerOffset = (labelHeightPx + iconGapPx) * 0.5f
                if (morph.morphMode() == LeftMorphMode.CollapseBurst) {
                    val centerT = FastOutSlowInEasing.transform(((c - 0.04f) / 0.44f).coerceIn(0f, 1f))
                    translationY = centerOffset * centerT
                } else {
                    val startCenterX = computeTabStartCenterX(morph, index)
                    val revealT = computeStreamRevealT(morph, startCenterX, p)
                    translationY = centerOffset * (1f - revealT)
                }
            },
        ) {
            icon()
        }
        Box(
            modifier = Modifier
                .onSizeChanged { labelHeightPx = it.height }
                .graphicsLayer {
                    if (morph == null) {
                        alpha = 1f
                        scaleX = 1f
                        scaleY = 1f
                        return@graphicsLayer
                    }
                    val p = morph.collapseProgress().coerceIn(0f, 1f)
                    val c = 1f - p
                    if (c <= 0.001f) {
                        alpha = 1f
                        scaleX = 1f
                        scaleY = 1f
                        return@graphicsLayer
                    }
                    if (morph.morphMode() == LeftMorphMode.CollapseBurst) {
                        val fadeT = (c / 0.18f).coerceIn(0f, 1f)
                        alpha = 1f - fadeT
                        val s = lerp(1f, 0.72f, fadeT)
                        scaleX = s
                        scaleY = s
                    } else {
                        val startCenterX = computeTabStartCenterX(morph, index)
                        val revealT = computeStreamRevealT(morph, startCenterX, p)
                        alpha = revealT
                        val s = lerp(0.72f, 1f, revealT)
                        scaleX = s
                        scaleY = s
                    }
                },
        ) {
            label()
        }
    }
}

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: GlassBackdrop,
    tabsCount: Int,
    modifier: Modifier = Modifier,
    containerHeight: Dp = 56.dp,
    selectorHeight: Dp = 48.dp,
    isExpanded: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    trailing: LiquidBottomTrailing? = null,
    fusion: LiquidFusion = LiquidFusion.Default,
    content: @Composable RowScope.() -> Unit
) {
    val isLightTheme = MiuixTheme.colorScheme.background.luminance() >= 0.5f
    val accentColor = if (isLightTheme) Color.Black else Color.White
    val containerColor = bottomBarSurfaceColor()

    val tabsBackdrop = rememberLayerBackdrop()

    val density = LocalDensity.current
    val animationScope = rememberCoroutineScope()
    val heightPx = with(density) { containerHeight.toPx() }
    val maxNeckHalfPx = heightPx * fusion.neckStrength
    val extraPx = trailing?.let {
        with(density) { (it.gap + containerHeight + it.dragRoom).toPx() }
    } ?: 0f
    val gapPx = trailing?.let { with(density) { it.gap.toPx() } } ?: 0f
    val pressExtraPx = with(density) { 16f.dp.toPx() }
    val maxIndex = (tabsCount - 1).toFloat()
    val collapseProgress = remember { Animatable(if (isExpanded) 1f else 0f) }
    val morphMode = remember {
        mutableStateOf(if (isExpanded) LeftMorphMode.ExpandStream else LeftMorphMode.CollapseBurst)
    }
    val jellyOvershoot = remember { Animatable(0f) }
    val maxJellyRadiusPx = with(density) { 2.8.dp.toPx() }
    val jellyRadiusDeltaPx: () -> Float = { jellyOvershoot.value * maxJellyRadiusPx }
    val latestIsExpanded by rememberUpdatedState(isExpanded)
    val latestOnExpandedChange by rememberUpdatedState(onExpandedChange)

    // 非对称展开（拉丝融条）/收起（碎滴聚球）联动：收起归球与展开融条末尾均带果冻回弹
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            val shouldJelly = collapseProgress.value < 0.95f
            if (jellyOvershoot.value != 0f) {
                launch { jellyOvershoot.animateTo(0f, tween(100)) }
            }
            if (collapseProgress.value <= 0.18f || morphMode.value == LeftMorphMode.ExpandStream) {
                morphMode.value = LeftMorphMode.ExpandStream
                if (collapseProgress.value < 0.999f) {
                    collapseProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = (580f * (1f - collapseProgress.value)).roundToInt().coerceAtLeast(140),
                            easing = LinearEasing,
                        ),
                    )
                } else {
                    collapseProgress.snapTo(1f)
                }
            } else {
                morphMode.value = LeftMorphMode.CollapseBurst
                if (collapseProgress.value < 0.999f) {
                    collapseProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = (280f * (1f - collapseProgress.value)).roundToInt().coerceAtLeast(120),
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    collapseProgress.snapTo(1f)
                }
            }
            if (shouldJelly) {
                jellyOvershoot.snapTo(0f)
                jellyOvershoot.animateTo(
                    targetValue = 0.55f,
                    animationSpec = tween(durationMillis = 60, easing = FastOutSlowInEasing),
                )
                jellyOvershoot.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.48f,
                        stiffness = 460f,
                    ),
                )
            }
        } else {
            val shouldJelly = collapseProgress.value > 0.05f
            if (collapseProgress.value >= 0.82f || morphMode.value == LeftMorphMode.CollapseBurst) {
                morphMode.value = LeftMorphMode.CollapseBurst
                if (collapseProgress.value > 0.001f) {
                    collapseProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = (760f * collapseProgress.value).roundToInt().coerceAtLeast(160),
                            easing = LinearEasing,
                        ),
                    )
                } else {
                    collapseProgress.snapTo(0f)
                }
            } else {
                morphMode.value = LeftMorphMode.ExpandStream
                if (collapseProgress.value > 0.001f) {
                    collapseProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = (320f * collapseProgress.value).roundToInt().coerceAtLeast(120),
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    collapseProgress.snapTo(0f)
                }
            }
            if (shouldJelly) {
                jellyOvershoot.snapTo(0f)
                jellyOvershoot.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 75, easing = FastOutSlowInEasing),
                )
                jellyOvershoot.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.40f,
                        stiffness = 420f,
                    ),
                )
            }
        }
    }

    val dampedDragAnimation = remember(animationScope) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedTabIndex().toFloat(),
            valueRange = 0f..maxIndex,
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 78f / 56f,
            onDragStarted = {},
            onDragStopped = {},
            onDrag = { _, _ -> }
        )
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.TopStart) {
        val viewConfiguration = LocalViewConfiguration.current
        val hapticFeedback = LocalHapticFeedback.current
        val padPx = with(density) { 4f.dp.toPx() }
        val pillPx = constraints.maxWidth.toFloat() - extraPx
        val pillWidthDp = with(density) { pillPx.toDp() }
        val tabWidth = (pillPx - padPx * 2f) / tabsCount
        val selectorInsetPx = with(density) { BottomBarSelectorInset.toPx() }

        val offsetAnimation = remember { Animatable(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (offsetAnimation.value / pillPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val ltrSign = if (isLtr) 1f else -1f
        var currentIndex by remember { mutableIntStateOf(selectedTabIndex()) }
        val latestSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { latestSelectedTabIndex() }
                .collectLatest { index ->
                    if (index != currentIndex) {
                        currentIndex = index
                    }
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val bleedX = with(density) { MaterialBleedX.toPx() }
        val bleedY = with(density) { MaterialBleedY.toPx() }
        val pressScaleOf: () -> Float = {
            lerp(1f, 1f + pressExtraPx / pillPx, dampedDragAnimation.pressProgress)
        }

        val trailingTopLeft = Offset(bleedX + pillPx + gapPx, bleedY)
        val trailingHighlight = if (trailing != null) {
            remember(animationScope, trailingTopLeft, containerHeight, isLightTheme) {
                InteractiveHighlight(
                    animationScope = animationScope,
                    position = { _, finger -> finger + trailingTopLeft },
                    radiusBaseDp = containerHeight,
                    fixedRadius = true,
                    drawFlatOverlay = false,
                    isDark = !isLightTheme,
                )
            }
        } else {
            null
        }

        val trailingTranslationOf: () -> Offset = {
            val d = trailingHighlight?.offset ?: Offset.Zero
            Offset(
                heightPx * tanh(0.025f * d.x / heightPx),
                heightPx * tanh(0.025f * d.y / heightPx),
            )
        }
        val trailingScaleOf: () -> Offset = {
            val progress = (trailingHighlight?.pressProgress ?: 0f).coerceAtLeast(0f)
            val scale = lerp(1f, 1f + with(density) { 4.dp.toPx() } / heightPx, progress)
            val d = trailingHighlight?.offset ?: Offset.Zero
            val maxDragScale = with(density) { 1.dp.toPx() } / heightPx
            val angle = atan2(d.y, d.x)
            Offset(
                scale + maxDragScale * abs(cos(angle) * d.x / heightPx),
                scale + maxDragScale * abs(sin(angle) * d.y / heightPx),
            )
        }

        val pillCenterInMaterial = Offset(bleedX + pillPx / 2f, bleedY + heightPx / 2f)
        val interactiveHighlight = remember(animationScope, bleedX, bleedY, pillPx, isLightTheme) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { _, finger ->
                    val s = pressScaleOf()
                    val local = finger + Offset(bleedX, bleedY) - Offset(panelOffset, 0f)
                    Offset(
                        (local.x - pillCenterInMaterial.x) / s + pillCenterInMaterial.x,
                        (local.y - pillCenterInMaterial.y) / s + pillCenterInMaterial.y
                    )
                },
                radiusBaseDp = containerHeight,
                fixedRadius = true,
                isDark = !isLightTheme,
            )
        }

        val chromeLens =
            AppMaterialSettings.chromeLensEnabled() && isRuntimeShaderSupported()
        val glassEffectsOn =
            AppMaterialSettings.glassEffectsEnabled() && isRenderEffectSupported()
        val glassBlur = AppMaterialSettings.glassBlurDp()
        val refractionShaderRef = remember { arrayOfNulls<GlassRuntimeShader>(1) }
        val refractionScaleRef = remember { floatArrayOf(1f) }
        val dropBufs = remember { Array(LEFT_BURST_DROPLET_COUNT) { FloatArray(4) } }
        val feedRefraction: () -> Unit =
            remember(
                pillPx, gapPx, heightPx, bleedX, bleedY, fusion, density,
                dampedDragAnimation, trailingHighlight, collapseProgress, morphMode, jellyOvershoot,
            ) {
                {
                    val sh = refractionShaderRef[0]
                    if (sh != null) {
                        val scale = refractionScaleRef[0]
                        val s = pressScaleOf()
                        val ts = trailingScaleOf()
                        val t = trailingTranslationOf()
                        val geom = computeLeftMorphGeometry(
                            leftX = bleedX,
                            centerY = bleedY + heightPx / 2f,
                            pillWidth = pillPx,
                            height = heightPx,
                            pressScale = s,
                            panelOffset = panelOffset,
                            collapseProgress = collapseProgress.value,
                            jellyRadiusDeltaPx = jellyRadiusDeltaPx(),
                            morphMode = morphMode.value,
                            density = density,
                        )
                        val pillHalfW = geom.halfWidth
                        val pillHalfH = geom.halfHeight
                        val pillCx = geom.centerX
                        val pillCy = geom.centerY
                        val circleCx = bleedX + pillPx + gapPx + heightPx / 2f + t.x
                        val circleCy = pillCy + t.y
                        val circleRx = heightPx / 2f * ts.x
                        val circleRy = heightPx / 2f * ts.y
                        val neck = if (geom.isFullyExpanded || geom.isFullyCollapsed) {
                            barNeckGeometryPx(
                                pillCenterX = pillCx,
                                pillCenterY = pillCy,
                                pillHalfWidth = pillHalfW,
                                pillHalfHeight = pillHalfH,
                                circleCenterX = circleCx,
                                circleCenterY = circleCy,
                                circleRadiusX = circleRx,
                                circleRadiusY = circleRy,
                                maxNeckHalfPx = maxNeckHalfPx,
                                fusion = fusion,
                                density = density,
                            )
                        } else {
                            null
                        }
                        val pillRadiusPx = minOf(pillHalfW, pillHalfH)
                        sh.setFloatUniform("offset", 0f, 0f)
                        sh.setFloatUniform("pillCenter", pillCx * scale, pillCy * scale)
                        sh.setFloatUniform("pillHalf", pillHalfW * scale, pillHalfH * scale)
                        sh.setFloatUniform("pillRadius", pillRadiusPx * scale)
                        sh.setFloatUniform("circleCenter", circleCx * scale, circleCy * scale)
                        sh.setFloatUniform("circleRadii", circleRx * scale, circleRy * scale)
                        sh.setFloatUniform("bulgeH", (neck?.bulgeH ?: 0f) * scale)
                        sh.setFloatUniform("bulgeAngle", neck?.bulgeAngle ?: 0f)
                        sh.setFloatUniform("bulgePhiPill", neck?.phiPill ?: 0f)
                        sh.setFloatUniform("bulgePhiCircle", neck?.phiCircle ?: 0f)
                        sh.setFloatUniform("bulgeGaussEdge", neck?.gaussEdge ?: 0f)
                        sh.setFloatUniform("bulgeSigma", fusion.bulgeSigma)

                        val shaderMorphMode = when {
                            geom.isFullyExpanded || geom.isFullyCollapsed -> 0f
                            geom.mode == LeftMorphMode.CollapseBurst -> 1f
                            else -> 2f
                        }
                        sh.setFloatUniform("morphMode", shaderMorphMode)
                        for (i in 0 until LEFT_BURST_DROPLET_COUNT) {
                            val d = geom.droplets[i]
                            val buf = dropBufs[i]
                            if (d.active && d.radius > 0f) {
                                buf[0] = d.cx * scale
                                buf[1] = d.cy * scale
                                buf[2] = d.radius * scale
                                buf[3] = 0f
                            } else {
                                buf[0] = 0f
                                buf[1] = 0f
                                buf[2] = 0f
                                buf[3] = 0f
                            }
                            sh.setFloatUniform(DROP_UNIFORM_NAMES[i], buf)
                        }
                        sh.setFloatUniform("dropSmoothK", geom.dropSmoothKPx * scale)

                        if (neck != null && neck.hasNeck) {
                            sh.setFloatUniform("pu", neck.pu.x * scale, neck.pu.y * scale)
                            sh.setFloatUniform("c1u", neck.c1u.x * scale, neck.c1u.y * scale)
                            sh.setFloatUniform("c2u", neck.c2u.x * scale, neck.c2u.y * scale)
                            sh.setFloatUniform("cu", neck.cu.x * scale, neck.cu.y * scale)
                            sh.setFloatUniform("pl", neck.pl.x * scale, neck.pl.y * scale)
                            sh.setFloatUniform("c1l", neck.c1l.x * scale, neck.c1l.y * scale)
                            sh.setFloatUniform("c2l", neck.c2l.x * scale, neck.c2l.y * scale)
                            sh.setFloatUniform("cl", neck.cl.x * scale, neck.cl.y * scale)
                            sh.setFloatUniform("neckRadius", 1f)
                        } else {
                            sh.setFloatUniform("neckRadius", 0f)
                        }
                        sh.setFloatUniform("refractionHeight", with(density) { 6f.dp.toPx() } * scale)
                        sh.setFloatUniform("refractionAmount", with(density) { -36f.dp.toPx() } * scale)
                    }
                }
            }
        val panelEffects: GlassEffectScope.() -> Unit =
            remember(
                chromeLens, glassEffectsOn, glassBlur,
                pillPx, gapPx, heightPx, bleedX, bleedY, fusion, density, feedRefraction,
            ) {
                {
                    if (glassEffectsOn) {
                        vibrancy()
                        blur(glassBlur.dp.toPx(), TileMode.Mirror)
                        padding = 0f
                        if (chromeLens) {
                            refractionScaleRef[0] = downsampleScale
                            refractionShaderRef[0] =
                                obtainShader("BottomBarRefraction", BottomBarRefractionShader)
                            runtimeShaderEffect(
                                key = "BottomBarRefraction",
                                shaderString = BottomBarRefractionShader,
                                uniformShaderName = "content",
                            ) {
                                refractionShaderRef[0] = this
                                feedRefraction()
                            }
                        }
                    }
                }
            }
        // 玻璃档下 containerColor 已按档位降过 alpha（浅 0.3 / 深 0.28），是薄底色不是实色罩：
        // 保图标可读，又不盖掉折射与边光。
        val panelSurface: DrawScope.() -> Unit =
            remember(containerColor) { { drawRect(containerColor) } }

        val shapeParams = remember(
            pillPx, extraPx, gapPx, heightPx, bleedX, bleedY, fusion,
            dampedDragAnimation, trailingHighlight, morphMode,
        ) {
            BarShapeParams(
                panelOffset = { panelOffset },
                pressScale = pressScaleOf,
                trailingTranslation = trailingTranslationOf,
                trailingScale = trailingScaleOf,
                extraPx = extraPx,
                gapPx = gapPx,
                maxNeckHalfPx = maxNeckHalfPx,
                fusion = fusion,
                bleedX = bleedX,
                bleedY = bleedY,
                collapseProgress = { collapseProgress.value },
                jellyRadiusDeltaPx = jellyRadiusDeltaPx,
                morphMode = { morphMode.value },
            )
        }
        val barEdgeShape: BarShape = remember(shapeParams) { BarShape(shapeParams) }
        val barShapeFactory: () -> Shape = remember(barEdgeShape) { { barEdgeShape.snapshotShape() } }
        val isSelectorLensActive by remember(dampedDragAnimation) {
            derivedStateOf { dampedDragAnimation.pressProgress > 0.001f }
        }
        val leftMorphShape = remember(bleedX, pillPx, heightPx, dampedDragAnimation, morphMode) {
            LeftMorphClipShape(
                panelOffset = { panelOffset },
                pressScale = pressScaleOf,
                collapseProgress = { collapseProgress.value },
                jellyRadiusDeltaPx = jellyRadiusDeltaPx,
                morphMode = { morphMode.value },
            )
        }
        val morphState = remember(tabsCount, tabWidth, padPx, heightPx, pillPx, isLtr, morphMode) {
            LiquidBottomBarMorphState(
                collapseProgress = { collapseProgress.value },
                jellyOvershoot = { jellyOvershoot.value },
                morphMode = { morphMode.value },
                selectedTabIndex = { currentIndex },
                tabsCount = tabsCount,
                tabWidthPx = tabWidth,
                padPx = padPx,
                orbDiameterPx = heightPx,
                pillWidthPx = pillPx,
                isLtr = isLtr,
            )
        }
        Box(
            Modifier
                .requiredSize(
                    width = with(density) { (constraints.maxWidth + bleedX * 2f).toDp() },
                    height = with(density) { (heightPx + bleedY * 2f).toDp() },
                )
                // 独立 OwnedLayer：contentVersion 更新或滑块联动时只失效底栏自身图层，不向上污染根容器
                .graphicsLayer {}
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = barShapeFactory,
                    effects = panelEffects,
                    onDrawSurface = panelSurface,
                )
                .then(interactiveHighlight.modifier)
                .then(trailingHighlight?.modifier ?: Modifier)
                .softGlassRim(barEdgeShape, isLightTheme)
        )

        CompositionLocalProvider(LocalLiquidBottomBarMorphState provides morphState) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(pillWidthDp)
                    .height(containerHeight)
                    .clip(leftMorphShape),
            ) {
                Row(
                    Modifier
                        .width(pillWidthDp)
                        .height(containerHeight)
                        .graphicsLayer {
                            val p = collapseProgress.value.coerceIn(0f, 1f)
                            translationX = panelOffset * p
                            val s = lerp(1f, pressScaleOf(), p)
                            scaleX = s
                            scaleY = s
                        }
                        .padding(4f.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }

            // 仅在手指按压/拖曳滑块需要透镜放大时才启用隐藏离屏模糊副本，静止与滚屏零开销
            if (isSelectorLensActive) {
                CompositionLocalProvider(
                    LocalLiquidBottomTabScale provides {
                        lerp(1f, 1.2f, dampedDragAnimation.pressProgress)
                    }
                ) {
                    Row(
                        Modifier
                            .clearAndSetSemantics {}
                            .requiredSize(
                                width = with(density) { (constraints.maxWidth + bleedX * 2f).toDp() },
                                height = with(density) { (heightPx + bleedY * 2f).toDp() },
                            )
                            .alpha(0f)
                            .layerBackdrop(tabsBackdrop)
                            .drawBackdrop(
                                backdrop = backdrop,
                                shape = barShapeFactory,
                                effects = {
                                    if (glassEffectsOn) {
                                        val progress = dampedDragAnimation.pressProgress
                                        vibrancy()
                                        blur(glassBlur.dp.toPx(), TileMode.Mirror)
                                        padding = 0f
                                        lens(
                                            24f.dp.toPx() * progress,
                                            24f.dp.toPx() * progress
                                        )
                                    }
                                },
                                onDrawSurface = {
                                    // 隐藏副本要和真正显示的那条同色，滑块采到的才是「底栏的玻璃」
                                    drawRect(containerColor)
                                    val progress = dampedDragAnimation.pressProgress
                                    if (progress > 0f) {
                                        drawRect(
                                            if (isLightTheme) Color.Black.copy(alpha = 0.06f)
                                            else Color.White.copy(alpha = 0.1f),
                                        )
                                    }
                                }
                            )
                            .then(interactiveHighlight.modifier)
                            .padding(
                                start = with(density) { (bleedX + 4.dp.toPx()).toDp() },
                                end = with(density) { (extraPx + bleedX + 4.dp.toPx()).toDp() },
                            )
                            .graphicsLayer {
                                val p = collapseProgress.value.coerceIn(0f, 1f)
                                translationX = panelOffset * p
                                colorFilter = ColorFilter.tint(accentColor)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        content = content
                    )
                }
            }
        }

        val selectorBaseModifier = Modifier
            .align(Alignment.CenterStart)
            .padding(horizontal = 4f.dp)
            .graphicsLayer {
                val p = collapseProgress.value.coerceIn(0f, 1f)
                val c = 1f - p
                val startX =
                    if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset + selectorInsetPx
                    else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset + selectorInsetPx
                val selectorW = (tabWidth - selectorInsetPx * 2f).coerceAtLeast(1f)
                val collapsedLeftX = heightPx * 0.5f - selectorW * 0.5f - padPx
                if (morphMode.value == LeftMorphMode.CollapseBurst) {
                    val fadeT = (c / 0.20f).coerceIn(0f, 1f)
                    translationX = lerp(startX, collapsedLeftX, FastOutSlowInEasing.transform(fadeT) * 0.35f)
                    alpha = 1f - fadeT
                } else {
                    val appearT = morphSmoothstep(0.60f, 0.90f, p)
                    val slideStart = (startX - tabWidth * 0.16f).coerceAtLeast(selectorInsetPx)
                    translationX = lerp(slideStart, startX, appearT)
                    alpha = appearT
                }
            }
            .graphicsLayer {
                val progress = dampedDragAnimation.pressProgress
                shadowElevation = 8.dp.toPx() * progress
                shape = ContinuousCapsule()
                clip = false
            }

        Box(
            selectorBaseModifier
                .then(
                    if (isSelectorLensActive) {
                        Modifier.drawBackdrop(
                            backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                            shape = { ContinuousCapsule() },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                lens(
                                    10f.dp.toPx() * progress,
                                    14f.dp.toPx() * progress,
                                    chromaticAberration = 1f
                                )
                            },
                            shadow = null,
                            layerBlock = {
                                val (sx, sy) = selectorScaleMultipliers(
                                    p = collapseProgress.value.coerceIn(0f, 1f),
                                    mode = morphMode.value,
                                )
                                scaleX = dampedDragAnimation.scaleX * sx
                                scaleY = dampedDragAnimation.scaleY * sy
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    if (isLightTheme) Color.Black.copy(0.06f)
                                    else Color.White.copy(0.1f),
                                    alpha = 1f - progress
                                )
                                drawRect(Color.Black.copy(alpha = 0.03f * progress))
                            }
                        )
                    } else {
                        Modifier
                            .graphicsLayer {
                                val (sx, sy) = selectorScaleMultipliers(
                                    p = collapseProgress.value.coerceIn(0f, 1f),
                                    mode = morphMode.value,
                                )
                                scaleX = dampedDragAnimation.scaleX * sx
                                scaleY = dampedDragAnimation.scaleY * sy
                                shape = ContinuousCapsule()
                                clip = true
                            }
                            .drawBehind {
                                drawRect(
                                    if (isLightTheme) Color.Black.copy(0.06f)
                                    else Color.White.copy(0.1f),
                                )
                            }
                    },
                )
                .innerShadow(
                    shape = ContinuousCapsule(),
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(radius = 8.dp * progress, alpha = progress)
                    },
                )
                .width(with(density) { (tabWidth - selectorInsetPx * 2f).toDp() })
                .height(selectorHeight)
        )

        if (trailing != null && trailingHighlight != null) {
            val trailingLeftDp = with(density) { (pillPx + gapPx).toDp() }
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .offset {
                        val t = trailingTranslationOf()
                        IntOffset(
                            with(density) { trailingLeftDp.toPx() }.roundToInt() + t.x.roundToInt(),
                            t.y.roundToInt(),
                        )
                    }
                    .size(containerHeight)
                    .then(trailingHighlight.gestureModifier)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            trailing.onClick()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    imageVector = trailing.icon,
                    contentDescription = trailing.contentDescription,
                    colorFilter = ColorFilter.tint(
                        trailing.iconTint
                            ?: if (isLightTheme) Color.Black.copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            val ts = trailingScaleOf()
                            scaleX = ts.x
                            scaleY = ts.y
                        },
                )
            }
        }

        Box(
            Modifier
                .layout { measurable, layoutConstraints ->
                    val w = lerp(heightPx, pillPx, collapseProgress.value.coerceIn(0f, 1f))
                        .roundToInt()
                        .coerceIn(layoutConstraints.minWidth, layoutConstraints.maxWidth)
                    val placeable = measurable.measure(layoutConstraints.copy(minWidth = w, maxWidth = w))
                    layout(w, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .fillMaxHeight()
                .then(interactiveHighlight.gestureModifier)
                .pointerInput(tabsCount, tabWidth, isLtr, padPx, pillPx) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downX = down.position.x
                        if (downX > size.width) return@awaitEachGesture

                        if (!latestIsExpanded || collapseProgress.value < 0.7f) {
                            // 收起态防误触：点击左侧玻璃球仅触发分裂展开，不触发 Tab 切换或拖拽
                            var movedTooFar = false
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || change.changedToUpIgnoreConsumed() || !change.pressed) {
                                    if (!movedTooFar && change != null) {
                                        change.consume()
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                        latestOnExpandedChange?.invoke(true)
                                    }
                                    break
                                }
                                val d = change.position - down.position
                                if (hypot(d.x, d.y) > touchSlop) {
                                    movedTooFar = true
                                }
                            }
                            return@awaitEachGesture
                        }

                        val capsuleCenterX = if (isLtr) {
                            padPx + (dampedDragAnimation.value + 0.5f) * tabWidth
                        } else {
                            pillPx - padPx - (dampedDragAnimation.value + 0.5f) * tabWidth
                        }
                        val capsuleHalf = tabWidth * 0.55f
                        var dragging = abs(downX - capsuleCenterX) <= capsuleHalf
                        var pressedTab = -1

                        if (dragging) {
                            dampedDragAnimation.press()
                        } else {
                            pressedTab = if (isLtr) {
                                floor((downX - padPx) / tabWidth).toInt()
                            } else {
                                floor((pillPx - padPx - downX) / tabWidth).toInt()
                            }.fastCoerceIn(0, tabsCount - 1)
                            dampedDragAnimation.animateToValueKeepingPress(pressedTab.toFloat())
                        }

                        var lastX = downX
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null || !change.pressed) {
                                val target = if (dragging) {
                                    dampedDragAnimation.targetValue
                                        .fastRoundToInt()
                                        .fastCoerceIn(0, tabsCount - 1)
                                } else {
                                    pressedTab.fastCoerceIn(0, tabsCount - 1)
                                }
                                currentIndex = target
                                dampedDragAnimation.animateToValue(target.toFloat())
                                dampedDragAnimation.release()
                                onTabSelected(target)
                                animationScope.launch {
                                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                                }
                                break
                            }

                            val dx = change.position.x - lastX
                            val totalDx = change.position.x - downX

                            if (!dragging && abs(totalDx) > touchSlop) {
                                dragging = true
                                lastX = change.position.x
                            }

                            if (dragging && abs(dx) > 0.01f) {
                                lastX = change.position.x
                                dampedDragAnimation.updateValue(
                                    (dampedDragAnimation.targetValue + dx / tabWidth * ltrSign)
                                        .fastCoerceIn(0f, maxIndex)
                                )
                                animationScope.launch {
                                    offsetAnimation.snapTo(offsetAnimation.value + dx)
                                }
                            } else {
                                lastX = change.position.x
                            }

                            change.consume()
                        }
                    }
                }
        )
    }
}