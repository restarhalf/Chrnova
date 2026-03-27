package restarhalf.stellar.schedule.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.Capsule
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.Clock

internal val LocalGlassNavigationBackdrop = staticCompositionLocalOf<Backdrop?> { null }

private val LocalGlassNavigationBarColors = staticCompositionLocalOf<GlassNavigationBarColors> {
    error("GlassNavigationBarColors is not provided")
}

private val LocalGlassNavigationBarRegistry = staticCompositionLocalOf<GlassNavigationBarRegistry?> { null }

private val LocalGlassNavigationBarTabScale = staticCompositionLocalOf { { 1f } }

private val LocalGlassNavigationBarPass = staticCompositionLocalOf { GlassNavigationBarPass.Primary }

private val LocalGlassNavigationBarActiveIndex = staticCompositionLocalOf { 0 }

private enum class GlassNavigationBarPass {
    Primary,
    Overlay,
}

@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    colors: GlassNavigationBarColors = GlassNavigationBarDefaults.colors(),
    showDivider: Boolean = false,
    defaultWindowInsetsPadding: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    horizontalOutSidePadding: Dp = GlassNavigationBarDefaults.HorizontalOutSidePadding,
    shadowElevation: Dp = GlassNavigationBarDefaults.ShadowElevation,
    content: @Composable RowScope.() -> Unit,
) {
    val contentState by rememberUpdatedState(content)
    val colorsState by rememberUpdatedState(colors)
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val isDarkTheme = isSystemInDarkTheme()

    val rootBackdrop = LocalGlassNavigationBackdrop.current
    val selfBackdrop = rememberLayerBackdrop()
    val backdrop = remember(rootBackdrop, selfBackdrop) { rootBackdrop ?: selfBackdrop }
    val tabsBackdrop = rememberLayerBackdrop()
    val barInteractionSource = remember { MutableInteractionSource() }

    val registry = remember { GlassNavigationBarRegistry() }
    val animationScope = rememberCoroutineScope()

    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    val tabCount = registry.count().coerceAtLeast(1)
    val selectedIndex = registry.selectedIndex().coerceIn(0, tabCount - 1)
    val selectedIndexState by rememberUpdatedState(selectedIndex)
    val registryState by rememberUpdatedState(registry)

    val horizontalContentPaddingPx = with(density) { GlassNavigationBarDefaults.ContentHorizontalPadding.toPx() }
    val contentWidthPx = remember(totalWidthPx, horizontalContentPaddingPx) {
        (totalWidthPx - horizontalContentPaddingPx * 2f).coerceAtLeast(0f)
    }
    val tabWidthPx = remember(contentWidthPx, tabCount) {
        if (tabCount > 0) contentWidthPx / tabCount else 0f
    }

    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(totalWidthPx, density) {
        derivedStateOf {
            if (totalWidthPx == 0f) {
                0f
            } else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }

    val capsule = remember { Capsule() }
    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }
    val holder = remember { DampedDragAnimationHolder() }

    val dampedDragAnimation = remember(animationScope, isLtr, tabCount) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = GlassNavigationBarDefaults.PressedIndicatorScale,
            canDrag = { offset ->
                val anim = holder.instance ?: return@DampedDragAnimation true
                if (tabWidthPx == 0f) {
                    return@DampedDragAnimation false
                }
                val currentValue = anim.value
                val indicatorX = currentValue * tabWidthPx
                val globalTouchX = if (isLtr) {
                    val touchX = indicatorX + offset.x
                    horizontalContentPaddingPx + touchX
                } else {
                    val touchX = totalWidthPx - horizontalContentPaddingPx - tabWidthPx - indicatorX + offset.x
                    touchX
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {},
            onDragStopped = {
                val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabCount - 1)
                animateToValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
                if (targetIndex != selectedIndexState && registryState.isEnabledAt(targetIndex)) {
                    registryState.performClickAt(targetIndex)
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabCount - 1).toFloat()),
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            },
        ).also { holder.instance = it }
    }

    val activeIndex by remember(tabCount, dampedDragAnimation) {
        derivedStateOf {
            dampedDragAnimation.value.fastRoundToInt().coerceIn(0, tabCount - 1)
        }
    }

    val interactiveHighlight = remember(animationScope, isLtr) {
        GlassInteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    if (isLtr) {
                        (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    } else {
                        size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    },
                    size.height / 2f,
                )
            },
        )
    }

    androidx.compose.runtime.LaunchedEffect(selectedIndex, tabCount, dampedDragAnimation) {
        val target = selectedIndex.coerceIn(0, tabCount - 1)
        dampedDragAnimation.animateToValue(target.toFloat())
    }

    val captionBarPadding = WindowInsets.captionBar.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val navigationBarPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPadding = remember(defaultWindowInsetsPadding, navigationBarPadding, captionBarPadding) {
        if (defaultWindowInsetsPadding) {
            if (navigationBarPadding > 0.dp || captionBarPadding > 0.dp) {
                GlassNavigationBarDefaults.BottomPaddingWithInset + navigationBarPadding + captionBarPadding
            } else {
                GlassNavigationBarDefaults.BottomPaddingWithoutInset
            }
        } else {
            0.dp
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (horizontalAlignment == Alignment.End) 0.dp else horizontalOutSidePadding,
                end = if (horizontalAlignment == Alignment.Start) 0.dp else horizontalOutSidePadding,
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = bottomPadding)
                .then(modifier)
                .fillMaxWidth()
                .align(horizontalAlignment)
                .onGloballyPositioned { coordinates ->
                    totalWidthPx = coordinates.size.width.toFloat()
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier
                    .then(
                        if (showDivider) {
                            Modifier
                                .background(colorsState.dividerColor, capsule)
                                .padding(GlassNavigationBarDefaults.DividerPadding)
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer {
                        if (shadowElevation > 0.dp) {
                            this.shadowElevation = with(density) { shadowElevation.toPx() }
                        }
                        shape = capsule
                        clip = true
                        translationX = panelOffset
                    }
                    .clickable(
                        role = Role.Tab,
                        indication = null,
                        interactionSource = barInteractionSource,
                        onClick = {},
                    )
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { capsule },
                        effects = {
                            vibrancy()
                            blur(GlassNavigationBarDefaults.BlurRadius.toPx())
                            lens(
                                GlassNavigationBarDefaults.LensRadiusX.toPx(),
                                GlassNavigationBarDefaults.LensRadiusY.toPx(),
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = 1f)
                        },
                        shadow = {
                            Shadow.Default.copy(
                                color = Color.Black.copy(
                                    if (isDarkTheme) 0.2f else 0.1f,
                                ),
                            )
                        },
                        layerBlock = {
                            val progress = dampedDragAnimation.pressProgress
                            val scale = lerp(
                                1f,
                                1f + GlassNavigationBarDefaults.PressedLayerExtraWidth.toPx() / size.width,
                                progress,
                            )
                            scaleX = scale
                            scaleY = scale
                        },
                        onDrawSurface = {
                            drawRect(colorsState.containerColor)
                        },
                    )
                    .then(interactiveHighlight.modifier)
                    .height(GlassNavigationBarDefaults.ContainerHeight)
                    .padding(horizontal = GlassNavigationBarDefaults.ContentHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(
                    LocalGlassNavigationBarColors provides colorsState,
                    LocalGlassNavigationBarRegistry provides registry,
                    LocalGlassNavigationBarPass provides GlassNavigationBarPass.Primary,
                    LocalGlassNavigationBarActiveIndex provides activeIndex,
                    LocalGlassNavigationBarTabScale provides {
                        lerp(1f, GlassNavigationBarDefaults.PressedTabScale, dampedDragAnimation.pressProgress)
                    },
                ) {
                    contentState()
                }
            }

            Row(
                modifier = Modifier
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer {
                        translationX = panelOffset
                    }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { capsule },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(GlassNavigationBarDefaults.BlurRadius.toPx())
                            lens(
                                GlassNavigationBarDefaults.LensRadiusX.toPx() * progress,
                                GlassNavigationBarDefaults.LensRadiusY.toPx() * progress,
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress)
                        },
                        onDrawSurface = {
                            drawRect(colorsState.containerColor)
                        },
                    )
                    .then(interactiveHighlight.modifier)
                    .height(GlassNavigationBarDefaults.IndicatorHeight)
                    .padding(horizontal = GlassNavigationBarDefaults.ContentHorizontalPadding)
                    .graphicsLayer(colorFilter = ColorFilter.tint(colorsState.indicatorTintColor)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(
                    LocalGlassNavigationBarColors provides colorsState,
                    LocalGlassNavigationBarRegistry provides null,
                    LocalGlassNavigationBarPass provides GlassNavigationBarPass.Overlay,
                    LocalGlassNavigationBarActiveIndex provides activeIndex,
                    LocalGlassNavigationBarTabScale provides {
                        lerp(1f, GlassNavigationBarDefaults.PressedTabScale, dampedDragAnimation.pressProgress)
                    },
                ) {
                    contentState()
                }
            }

            if (tabWidthPx > 0f) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = GlassNavigationBarDefaults.ContentHorizontalPadding)
                        .graphicsLayer {
                            val progressOffset = dampedDragAnimation.value * tabWidthPx
                            translationX = if (isLtr) {
                                progressOffset + panelOffset
                            } else {
                                -progressOffset + panelOffset
                            }
                        }
                        .then(interactiveHighlight.gestureModifier)
                        .then(dampedDragAnimation.modifier)
                        .drawBackdrop(
                            backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                            shape = { capsule },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                lens(
                                    GlassNavigationBarDefaults.IndicatorLensRadiusX.toPx() * progress,
                                    GlassNavigationBarDefaults.IndicatorLensRadiusY.toPx() * progress,
                                    chromaticAberration = true,
                                )
                            },
                            highlight = {
                                Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress)
                            },
                            shadow = {
                                Shadow(alpha = dampedDragAnimation.pressProgress)
                            },
                            innerShadow = {
                                InnerShadow(
                                    radius = GlassNavigationBarDefaults.InnerShadowRadius * dampedDragAnimation.pressProgress,
                                    alpha = dampedDragAnimation.pressProgress,
                                )
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                val progress = dampedDragAnimation.pressProgress
                                drawRect(
                                    color = colorsState.indicatorSurfaceColor,
                                    alpha = 1f - progress,
                                )
                                drawRect(
                                    Color.Black.copy(alpha = 0.03f * progress),
                                )
                            },
                        )
                        .height(GlassNavigationBarDefaults.IndicatorHeight)
                        .size(
                            width = with(density) { tabWidthPx.toDp() },
                            height = GlassNavigationBarDefaults.IndicatorHeight,
                        ),
                )
            }
        }
    }
}

@Composable
fun RowScope.GlassNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDirectionalIcon: Boolean = false,
) {
    val onClickState by rememberUpdatedState(onClick)
    val registry = LocalGlassNavigationBarRegistry.current
    val colors = LocalGlassNavigationBarColors.current
    val pass = LocalGlassNavigationBarPass.current
    val activeIndex = LocalGlassNavigationBarActiveIndex.current
    val tabScaleProvider = LocalGlassNavigationBarTabScale.current
    val layoutDirection = LocalLayoutDirection.current

    val itemId = remember { Any() }
    if (pass == GlassNavigationBarPass.Primary) {
        DisposableEffect(registry, itemId) {
            registry?.register(itemId)
            onDispose {
                registry?.unregister(itemId)
            }
        }
        androidx.compose.runtime.SideEffect {
            registry?.update(itemId, selected = selected, enabled = enabled) {
                onClickState()
            }
        }
    }

    val itemIndex by remember(registry, itemId) {
        derivedStateOf { registry?.indexOf(itemId) ?: -1 }
    }
    val isActive = remember(pass, itemIndex, activeIndex) {
        pass == GlassNavigationBarPass.Primary && itemIndex == activeIndex
    }

    var isPressed by remember(itemId) { mutableStateOf(false) }
    val itemScale by remember(tabScaleProvider) {
        derivedStateOf { tabScaleProvider() }
    }
    val contentColor = remember(colors, isActive, enabled, isPressed) {
        colors.itemColor(
            selected = isActive,
            enabled = enabled,
            pressed = isPressed,
        )
    }

    val itemPointerModifier = remember(pass, enabled, onClickState) {
        if (pass == GlassNavigationBarPass.Primary) {
            Modifier.pointerInput(enabled, onClickState) {
                detectTapGestures(
                    onPress = {
                        if (enabled) {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    },
                    onTap = {
                        if (enabled) {
                            onClickState()
                        }
                    },
                )
            }
        } else {
            Modifier
        }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .weight(1f)
            .clip(Capsule())
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .then(itemPointerModifier),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            modifier = Modifier
                .size(GlassNavigationBarDefaults.IconSize)
                .graphicsLayer {
                    scaleX =
                        if (isDirectionalIcon) {
                            if (layoutDirection == LayoutDirection.Rtl) -1f else 1f
                        } else {
                            1f
                        }
                },
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
        )

        Text(
            modifier = Modifier.padding(top = GlassNavigationBarDefaults.LabelTopPadding),
            text = label,
            color = contentColor,
            fontSize = GlassNavigationBarDefaults.LabelFontSize,
        )
    }
}

@Immutable
class GlassNavigationBarColors(
    val containerColor: Color,
    val dividerColor: Color,
    val selectedContentColor: Color,
    val unselectedContentColor: Color,
    val disabledContentColor: Color,
    val selectedPressedContentColor: Color,
    val unselectedPressedContentColor: Color,
    val indicatorTintColor: Color,
    val indicatorSurfaceColor: Color,
) {

    @Stable
    fun itemColor(
        selected: Boolean,
        enabled: Boolean,
        pressed: Boolean,
    ): Color {
        if (!enabled) {
            return disabledContentColor
        }
        return when {
            pressed && selected -> selectedPressedContentColor
            pressed && !selected -> unselectedPressedContentColor
            selected -> selectedContentColor
            else -> unselectedContentColor
        }
    }
}

object GlassNavigationBarDefaults {
    val HorizontalOutSidePadding = 15.dp

    val ShadowElevation = 1.dp

    val ContentHorizontalPadding = 4.dp

    val BottomPaddingWithInset = 8.dp

    val BottomPaddingWithoutInset = 36.dp

    val DividerPadding = 0.75.dp

    val ContainerHeight = 64.dp

    val IndicatorHeight = 56.dp

    val IconSize = 24.dp

    val LabelTopPadding = 2.dp

    val LabelFontSize = 12.sp

    val BlurRadius = 8.dp

    val LensRadiusX = 24.dp

    val LensRadiusY = 24.dp

    val IndicatorLensRadiusX = 10.dp

    val IndicatorLensRadiusY = 14.dp

    val InnerShadowRadius = 8.dp

    val PressedTabScale = 1.2f

    val PressedIndicatorScale = 78f / 56f

    val PressedLayerExtraWidth = 16.dp

    @Composable
    fun colors(
        containerColor: Color = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f),
        dividerColor: Color = MiuixTheme.colorScheme.dividerLine,
        selectedContentColor: Color = MiuixTheme.colorScheme.primary,
        unselectedContentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.65f),
        disabledContentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.28f),
        selectedPressedContentColor: Color = MiuixTheme.colorScheme.primary.copy(alpha = 0.62f),
        unselectedPressedContentColor: Color = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.48f),
        indicatorTintColor: Color = MiuixTheme.colorScheme.primary,
        indicatorSurfaceColor: Color = Color.Unspecified,
    ): GlassNavigationBarColors {
        val resolvedIndicatorSurfaceColor = if (indicatorSurfaceColor.isSpecified) {
            indicatorSurfaceColor
        } else if (isSystemInDarkTheme()) {
            Color.White.copy(alpha = 0.12f)
        } else {
            Color.Black.copy(alpha = 0.1f)
        }
        return remember(
            containerColor,
            dividerColor,
            selectedContentColor,
            unselectedContentColor,
            disabledContentColor,
            selectedPressedContentColor,
            unselectedPressedContentColor,
            indicatorTintColor,
            resolvedIndicatorSurfaceColor,
        ) {
            GlassNavigationBarColors(
                containerColor = containerColor,
                dividerColor = dividerColor,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                disabledContentColor = disabledContentColor,
                selectedPressedContentColor = selectedPressedContentColor,
                unselectedPressedContentColor = unselectedPressedContentColor,
                indicatorTintColor = indicatorTintColor,
                indicatorSurfaceColor = resolvedIndicatorSurfaceColor,
            )
        }
    }
}

private class GlassNavigationBarRegistry {
    private val ids = mutableStateListOf<Any>()
    private val nodes = linkedMapOf<Any, GlassNavigationBarItemNode>()

    fun register(id: Any) {
        if (!ids.contains(id)) {
            ids.add(id)
        }
        if (nodes[id] == null) {
            nodes[id] = GlassNavigationBarItemNode()
        }
    }

    fun unregister(id: Any) {
        ids.remove(id)
        nodes.remove(id)
    }

    fun update(
        id: Any,
        selected: Boolean,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        val node = nodes[id] ?: return
        node.selected = selected
        node.enabled = enabled
        node.onClick = onClick
    }

    fun count(): Int = ids.size

    fun indexOf(id: Any): Int = ids.indexOf(id)

    fun selectedIndex(): Int {
        val index = ids.indexOfFirst { id -> nodes[id]?.selected == true }
        return if (index >= 0) index else 0
    }

    fun isEnabledAt(index: Int): Boolean {
        val itemId = ids.getOrNull(index) ?: return false
        return nodes[itemId]?.enabled == true
    }

    fun performClickAt(index: Int) {
        val itemId = ids.getOrNull(index) ?: return
        val node = nodes[itemId] ?: return
        if (node.enabled) {
            node.onClick.invoke()
        }
    }
}

private class GlassNavigationBarItemNode {
    var selected by mutableStateOf(false)
    var enabled by mutableStateOf(true)
    var onClick: () -> Unit = {}
}

private class DampedDragAnimation(
    private val animationScope: kotlinx.coroutines.CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val canDrag: (Offset) -> Boolean = { true },
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: androidx.compose.ui.unit.IntSize, dragAmount: Offset) -> Unit,
) {

    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                onDragStarted(down.position)
                press()
            },
            onDragEnd = {
                onDragStopped()
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            },
        ) { change, dragAmount ->
            val position = change.position
            val previousPosition = change.previousPosition
            val isInside = canDrag(position)
            val wasInside = canDrag(previousPosition)
            if (isInside && wasInside) {
                onDrag(size, dragAmount)
            }
        }
    }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            withFrameNanos { }
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val target = value.coerceIn(valueRange)
        animationScope.launch {
            launch {
                valueAnimation.animateTo(target, valueAnimationSpec) {
                    updateVelocity()
                }
            }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val target = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(target, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            Clock.System.now().toEpochMilliseconds(),
            Offset(value, 0f),
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch {
            velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec)
        }
    }
}

private class GlassInteractiveHighlight(
    private val animationScope: kotlinx.coroutines.CoroutineScope,
    private val position: (size: Size, offset: Offset) -> Offset,
) {
    private val pressProgressAnimationSpec = spring(0.5f, 300f, 0.001f)
    private val positionAnimationSpec = spring(0.5f, 300f, Offset.VisibilityThreshold)

    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val positionAnimation = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    private var startPosition = Offset.Zero

    val modifier: Modifier = Modifier.drawWithContent {
        val progress = pressProgressAnimation.value
        if (progress > 0f) {
            drawRect(
                Color.White.copy(alpha = 0.06f * progress),
                blendMode = BlendMode.Plus,
            )
            val center = position(size, positionAnimation.value)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f * progress),
                        Color.Transparent,
                    ),
                    center = Offset(
                        center.x.fastCoerceIn(0f, size.width),
                        center.y.fastCoerceIn(0f, size.height),
                    ),
                    radius = size.minDimension * 1.2f,
                ),
                blendMode = BlendMode.Plus,
            )
        }
        drawContent()
    }

    val gestureModifier: Modifier = Modifier.pointerInput(animationScope) {
        inspectDragGestures(
            onDragStart = { down ->
                startPosition = down.position
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
                    launch { positionAnimation.snapTo(startPosition) }
                }
            },
            onDragEnd = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            },
            onDragCancel = {
                animationScope.launch {
                    launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
                    launch { positionAnimation.animateTo(startPosition, positionAnimationSpec) }
                }
            },
        ) { change, _ ->
            animationScope.launch {
                positionAnimation.snapTo(change.position)
            }
        }
    }
}

private suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val down = awaitFirstDown(requireUnconsumed = false)

        onDragStart(down)
        onDrag(initialDown, Offset.Zero)

        val upEvent = drag(pointerId = initialDown.id) { change ->
            onDrag(change, change.positionChange())
        }

        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }

    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) {
            return null
        }
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            }
            pointer = otherDown.id
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}
