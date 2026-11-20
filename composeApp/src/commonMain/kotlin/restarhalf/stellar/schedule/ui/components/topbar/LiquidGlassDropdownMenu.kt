package restarhalf.stellar.schedule.ui.components.topbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick as onSemanticsClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.haooz.softglass.GlassPreset
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur as backdropBlur
import com.haooz.softglass.backdrop.effects.runtimeShaderEffect
import com.haooz.softglass.glass.BrightLargeTone
import com.haooz.softglass.glass.ThemeFill
import com.haooz.softglass.glass.applySoftGlass
import com.haooz.softglass.shape.ContinuousRoundedRectangle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.ui.components.EDGE_SAFE_SOFT_GLASS_LIVE_SHADER
import restarhalf.stellar.schedule.ui.components.animation.InteractiveHighlight
import restarhalf.stellar.schedule.ui.modifier.softGlassRim
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.interactive.DropdownPanelDragSelectState
import top.yukonga.miuix.kmp.interactive.LocalDropdownPanelDragSelect
import top.yukonga.miuix.kmp.interactive.computeDragTransform
import top.yukonga.miuix.kmp.interactive.dropdownPanelDragSelect
import top.yukonga.miuix.kmp.interactive.dropdownPanelEntry
import top.yukonga.miuix.kmp.internal.drawBlurredPathShadow
import top.yukonga.miuix.kmp.material.LocalGlassBlurDp
import top.yukonga.miuix.kmp.theme.LocalForcedDarkTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.rememberAppSettingDark
import top.yukonga.miuix.kmp.utils.rememberNavigationBack

val LiquidGlassDropdownShadowPadding = 24.dp

private val ButtonDiameter = 42.dp

private val PanelWidth = 200.dp

private val PanelCornerRadius = 25.dp

/** 下拉面板玻璃的 shader 缓存键。 */
private const val DROPDOWN_GLASS_KEY = "{{SECRET_p9jwz3na}}"

private val ShadowColorLight = Color(0x1A000000)
private val ShadowColorDark = Color(0x2E000000)
private const val ShadowBlur = 12f
private const val ShadowExtend = 2f
private const val ShadowAlphaGain = 3.2f

@Composable
fun LiquidGlassDropdownMenu(
    show: Boolean,
    backdrop: GlassBackdrop,
    modifier: Modifier = Modifier,
    fraction: Animatable<Float, *> = remember { Animatable(0f) },
    onDismiss: (() -> Unit)? = null,
    onBackCancelled: (() -> Unit)? = null,
    triggerIcon: ImageVector? = null,
    triggerIconSize: Dp = 23.dp,
    triggerContentDescription: String? = null,
    onExpand: (() -> Unit)? = null,
    offsetPx: () -> Offset = { Offset.Zero },
    materialAlpha: () -> Float = { 1f },
    content: @Composable ColumnScope.() -> Unit,
) {
    val appDark = LocalForcedDarkTheme.current ?: rememberAppSettingDark()
    val isLightTheme = !appDark
    val containerColor =
        if (MiuixTheme.isDynamicColor) {
            MiuixTheme.colorScheme.surfaceContainer.copy(
                alpha =
                    AppMaterialSettings.glassSurfaceAlpha(
                        if (isLightTheme) 0.76f else 0.84f,
                        isLightTheme,
                    ),
            )
        } else if (isLightTheme) {
            Color(0xFFFAFAFA).copy(
                alpha = AppMaterialSettings.glassSurfaceAlpha(0.76f, true),
            )
        } else {
            Color(0xFF242424).copy(
                alpha = AppMaterialSettings.glassSurfaceAlpha(0.84f, false),
            )
        }
    val triggerIconTint =
        if (isLightTheme) {
            Color.Black.copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.85f)
        }
    val hapticFeedback = LocalHapticFeedback.current
    val originProgress = remember { Animatable(0f) }
    val settleBounce = remember { Animatable(1f) }
    var hasOpened by remember { mutableStateOf(false) }
    val dragSelectState = remember { DropdownPanelDragSelectState() }

    val navBackState = rememberNavigationBack()
    val backProgress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val interactiveHighlight =
        remember(coroutineScope, appDark) {
            InteractiveHighlight(
                animationScope = coroutineScope,
                radiusScale = { 1f - 0.6f * fraction.value.coerceIn(0f, 1f) },
                radiusBaseDp = 150.dp,
                isDark = appDark,
            )
        }

    navBackState.handler(
        enabled = show && onDismiss != null,
        onBackCancelled = {
            onBackCancelled?.invoke()
            coroutineScope.launch {
                if (backProgress.value > 0f) {
                    fraction.animateTo(1f, animationSpec = tween(150))
                    originProgress.animateTo(1f, animationSpec = tween(150))
                    backProgress.snapTo(0f)
                }
            }
        },
        onBackCompleted = {
            onDismiss?.invoke()
        },
    )

    LaunchedEffect(Unit) {
        snapshotFlow { navBackState.backProgress() }
            .collect { progress ->
                if (progress != null) {
                    backProgress.snapTo(progress)
                    fraction.snapTo(1f - progress)
                    originProgress.snapTo(1f - progress)
                }
            }
    }

    LaunchedEffect(show) {
        if (show) {
            hasOpened = true
            launch {
                fraction.animateTo(
                    1f,
                    spring(dampingRatio = 0.77f, stiffness = 220f, visibilityThreshold = 0.0001f),
                )
            }
            launch {
                originProgress.animateTo(
                    1f,
                    spring(dampingRatio = 0.77f, stiffness = 420f, visibilityThreshold = 0.0001f),
                )
            }
            settleBounce.snapTo(1f)
        } else if (!hasOpened) {
            fraction.snapTo(0f)
            originProgress.snapTo(0f)
            settleBounce.snapTo(0f)
        } else {
            val exitEasing = CubicBezierEasing(0f, 0f, 0f, 1f)
            launch {
                fraction.animateTo(
                    0f,
                    spring(dampingRatio = 0.85f, stiffness = 650f, visibilityThreshold = 0.0001f),
                )
            }
            launch {
                settleBounce.snapTo(1f)
                settleBounce.animateTo(
                    0f,
                    spring(dampingRatio = 0.52f, stiffness = 220f, visibilityThreshold = 0.0001f),
                )
            }
            originProgress.animateTo(0f, tween(340, easing = exitEasing))
            fraction.snapTo(0f)
            originProgress.snapTo(0f)
        }
    }

    val f = fraction.value
    val blurStep = (f.coerceIn(0f, 1f) * 8f).roundToInt()
    val expandStep = blurStep / 8f
    // 展开过程中模糊从「玻璃件档」爬到「面板档」，与展开进度同步
    val blurDp =
        if (!AppMaterialSettings.glassAvailable()) {
            0f
        } else {
            LocalGlassBlurDp.current +
                (AppMaterialSettings.glassPanelBlurDp() - LocalGlassBlurDp.current) * expandStep
        }
    var panelHeightPx by remember { mutableIntStateOf(0) }
    val targetHeight =
        if (panelHeightPx > 0) {
            with(LocalDensity.current) { panelHeightPx.toDp() }
        } else {
            ButtonDiameter
        }
    val materialVisible: () -> Float = {
        max(materialAlpha(), (fraction.value * 5f).coerceIn(0f, 1f))
    }
    val contentBlur = 6.dp * (1f - abs(2f * f - 1f)).coerceIn(0f, 1f)
    val width = lerp(ButtonDiameter, PanelWidth, f)
    val height = lerp(ButtonDiameter, targetHeight, f)
    val contentScale = (width.value / PanelWidth.value).coerceAtLeast(0f)
    // 页面自己画触发按钮时（triggerIcon == null），收起状态下这一层不能拦截触摸，
    // 否则会挡住顶栏里真正可点的「更多」按钮。
    val panelInteractive = show || f > 0f || triggerIcon != null

    Box(modifier = modifier.padding(LiquidGlassDropdownShadowPadding)) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        val o = offsetPx()
                        val p = originProgress.value
                        val s = size.width / PanelWidth.toPx()
                        val k = 0.5f * p * (1f - s)
                        val pulse = (-settleBounce.value).coerceAtLeast(0f)
                        val bouncePx = pulse * 12f * density
                        val drag =
                            computeDragTransform(
                                width = size.width,
                                height = size.height,
                                fraction = f,
                                pressProgress = interactiveHighlight.pressProgress,
                                offset = interactiveHighlight.offset,
                                density = this,
                            )
                        scaleX = drag.scaleX
                        scaleY = drag.scaleY
                        translationX = o.x - PanelWidth.toPx() * k + bouncePx + drag.translationX
                        translationY = o.y + targetHeight.toPx() * k - bouncePx + drag.translationY
                    }
                    .size(width = width, height = height)
                    .onGloballyPositioned { dragSelectState.panelTopInRoot = it.boundsInRoot().top }
                    .then(
                        if (panelInteractive) {
                            Modifier
                                .dropdownPanelDragSelect(
                                    state = dragSelectState,
                                    fraction = { fraction.value },
                                    hapticFeedback = hapticFeedback,
                                )
                                .then(interactiveHighlight.gestureModifier)
                        } else {
                            Modifier
                        },
                    )
                    .drawBehind {
                        val spread = materialVisible()
                        if (spread > 0.01f) {
                            val shadowColor = if (isLightTheme) ShadowColorLight else ShadowColorDark
                            val blurRadius = ShadowBlur * density * spread
                            val shadowSpread = ShadowExtend * density * spread
                            val r = PanelCornerRadius.toPx()
                            val ringPath =
                                Path().apply {
                                    addRoundRect(
                                        RoundRect(
                                            rect =
                                                Rect(
                                                    -shadowSpread,
                                                    -shadowSpread,
                                                    size.width + shadowSpread,
                                                    size.height + shadowSpread,
                                                ),
                                            cornerRadius = CornerRadius(r + shadowSpread, r + shadowSpread),
                                        ),
                                        Path.Direction.Clockwise,
                                    )
                                    addRoundRect(
                                        RoundRect(
                                            rect = Rect(0f, 0f, size.width, size.height),
                                            cornerRadius = CornerRadius(r, r),
                                        ),
                                        Path.Direction.CounterClockwise,
                                    )
                                }
                            drawBlurredPathShadow(
                                path = ringPath,
                                color =
                                    shadowColor.copy(
                                        alpha = (shadowColor.alpha * ShadowAlphaGain).coerceAtMost(1f),
                                    ),
                                blurRadius = blurRadius.coerceAtLeast(0.1f),
                            )
                        }
                    }
                    .clip(ContinuousRoundedRectangle(PanelCornerRadius)),
        ) {
            val glassEffectsOn = AppMaterialSettings.glassEffectsEnabled()
            val glassStyle =
                AppMaterialSettings.chromeGlassStyle(
                    base = GlassPreset.Default.style,
                    isDark = !isLightTheme,
                    fill = ThemeFill,
                    tone = BrightLargeTone,
                )
            val panelCornerRadiusPx =
                with(LocalDensity.current) { PanelCornerRadius.toPx() }
            val glassEffects: GlassEffectScope.() -> Unit =
                remember(glassEffectsOn, blurStep, blurDp, glassStyle, panelCornerRadiusPx) {
                    {
                        if (glassEffectsOn) {
                            backdropBlur(
                                blurDp.dp.toPx(),
                                androidx.compose.ui.graphics.TileMode.Mirror,
                            )
                            padding = 0f
                            runtimeShaderEffect(
                                key = DROPDOWN_GLASS_KEY,
                                shaderString = EDGE_SAFE_SOFT_GLASS_LIVE_SHADER,
                                uniformShaderName = "content",
                            ) {
                                applySoftGlass(
                                    style = glassStyle,
                                    width = size.width,
                                    height = size.height,
                                    radiusPx = panelCornerRadiusPx,
                                )
                            }
                        }
                    }
                }
            val panelShape = remember { ContinuousRoundedRectangle(PanelCornerRadius) }
            val panelShapeBlock: () -> androidx.compose.ui.graphics.Shape =
                remember(panelShape) { { panelShape } }
            val panelSurface: (androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit)? =
                remember(glassEffectsOn, containerColor) {
                    if (glassEffectsOn) null else { { drawRect(containerColor) } }
                }
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = materialVisible() }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = panelShapeBlock,
                            effects = glassEffects,
                            shadow = null,
                            onDrawSurface = panelSurface,
                        )
                        .softGlassRim(panelShape, isLightTheme),
            )

            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .then(interactiveHighlight.modifier),
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .requiredWidth(PanelWidth)
                        .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
                        .onSizeChanged { panelHeightPx = it.height }
                        .graphicsLayer {
                            alpha = ((f - 0.3f) / 0.4f).coerceIn(0f, 1f)
                            scaleX = contentScale
                            scaleY = contentScale
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                        }
                        .blur(contentBlur)
                        .padding(vertical = 8.dp),
            ) {
                CompositionLocalProvider(LocalDropdownPanelDragSelect provides dragSelectState) {
                    content()
                }
            }

            if (triggerIcon != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(ButtonDiameter)
                            .clip(CircleShape)
                            .graphicsLayer {
                                val iconK = if (show) 2.5f else 1.7f
                                val p = (fraction.value * iconK).coerceIn(0f, 1f)
                                alpha = 1f - p
                                translationX = p * (ButtonDiameter.toPx() / 2f - width.toPx() / 2f)
                                translationY = p * (height.toPx() / 2f - ButtonDiameter.toPx() / 2f)
                            }
                            .clickable(
                                enabled = !show && onExpand != null,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                                onExpand?.invoke()
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = triggerIcon,
                        contentDescription = triggerContentDescription,
                        modifier = Modifier.size(triggerIconSize),
                        tint = triggerIconTint,
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidGlassDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
) {
    val appDark = LocalForcedDarkTheme.current ?: rememberAppSettingDark()
    val textColor = if (!appDark) Color(0xFF1A1A1A) else Color(0xFFE8E4DE)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(ContinuousRoundedRectangle(17.dp))
                .dropdownPanelEntry(enabled = true, action = onClick)
                .semantics {
                    role = Role.Button
                    onSemanticsClick {
                        onClick()
                        true
                    }
                }
                .padding(horizontal = 14.dp, vertical = 10.5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.size(10.dp))
            }
            Text(
                text = text,
                fontSize = 15.6.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
            )
        }
    }
}
