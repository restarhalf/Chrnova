package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import com.haooz.softglass.GlassPreset
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.backdrop.effects.runtimeShaderEffect
import com.haooz.softglass.glass.BrightTone
import com.haooz.softglass.glass.ThemeFill
import com.haooz.softglass.glass.applySoftGlass
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import restarhalf.stellar.schedule.ui.components.animation.InteractiveHighlight
import restarhalf.stellar.schedule.ui.modifier.softGlassRim
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.overlay.LocalSheetContentBackdrop
import top.yukonga.miuix.kmp.overlay.LocalSheetTopBarMaterial
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 弹窗内玻璃圆钮的 shader 缓存键。 */
private const val SHEET_BUTTON_GLASS_KEY = "ChrnovaSoftGlassSheetButton"

@Composable
fun SheetGlassButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 42.dp,
    iconSize: Dp = 24.dp,
) {
    val animationScope = rememberCoroutineScope()
    val highlight = remember(animationScope) { InteractiveHighlight(animationScope) }
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val backdrop = LocalSheetContentBackdrop.current
    val topBarMaterial = LocalSheetTopBarMaterial.current
    val isLightTheme = MiuixTheme.colorScheme.background.luminance() >= 0.5f
    val containerColor =
        if (MiuixTheme.isDynamicColor) {
            MiuixTheme.colorScheme.surfaceContainerHigh.copy(
                alpha =
                    AppMaterialSettings.glassSurfaceAlpha(
                        if (isLightTheme) 0.6f else 0.4f,
                        isLightTheme,
                    ),
            )
        } else if (isLightTheme) {
            Color.White.copy(alpha = AppMaterialSettings.glassSurfaceAlpha(0.6f, true))
        } else {
            Color(0xFF363636).copy(
                alpha = AppMaterialSettings.glassSurfaceAlpha(0.4f, false),
            )
        }

    val shapeProvider: () -> Shape = remember { { CircleShape } }
    val glassEffectsOn = AppMaterialSettings.glassEffectsEnabled()
    val glassBlur = AppMaterialSettings.glassBlurDp()
    val glassStyle =
        AppMaterialSettings.chromeGlassStyle(
            base = GlassPreset.Default.style,
            isDark = !isLightTheme,
            fill = ThemeFill,
            tone = BrightTone,
        )
    val effects: GlassEffectScope.() -> Unit =
        remember(glassEffectsOn, glassBlur, glassStyle) {
            {
                if (glassEffectsOn) {
                    blur(glassBlur.dp.toPx(), TileMode.Mirror)
                    padding = 0f
                    runtimeShaderEffect(
                        key = SHEET_BUTTON_GLASS_KEY,
                        shaderString = EDGE_SAFE_SOFT_GLASS_LIVE_SHADER,
                        uniformShaderName = "content",
                    ) {
                        applySoftGlass(style = glassStyle, width = size.width, height = size.height)
                    }
                }
            }
        }
    val surface: DrawScope.() -> Unit = remember(containerColor) { { drawRect(containerColor) } }

    Box(
        modifier =
            modifier
                .wrapContentSize()
                .size(buttonSize)
                .graphicsLayer {
                    val progress = highlight.pressProgress.coerceAtLeast(0f)
                    val scale = 1f + 4.dp.toPx() / size.height * progress
                    val offset = highlight.offset
                    val maxDragScale = 1.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX =
                        scale +
                            maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                            (size.width / size.height).fastCoerceAtMost(1f)
                    scaleY =
                        scale +
                            maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                            (size.height / size.width).fastCoerceAtMost(1f)
                    val contentMin = size.minDimension.coerceAtLeast(1f)
                    translationX = contentMin * tanh(0.08f * offset.x / contentMin)
                    translationY = contentMin * tanh(0.08f * offset.y / contentMin)
                },
        contentAlignment = Alignment.Center,
    ) {
        if (backdrop != null) {
            Box(
                modifier =
                    Modifier
                        .size(buttonSize)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = shapeProvider,
                            effects = effects,
                            shadow = null,
                            layerBlock = { alpha = topBarMaterial.backdropAlpha },
                            onDrawSurface = if (glassEffectsOn) null else surface,
                        )
                        .softGlassRim(CircleShape, isLightTheme),
            )
        } else {
            Box(modifier = Modifier.size(buttonSize).clip(CircleShape).background(containerColor))
        }
        Box(
            modifier =
                Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .then(highlight.modifier)
                    .then(highlight.gestureModifier)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            onClick()
                        },
                    ),
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint =
                if (isLightTheme) {
                    Color.Black.copy(alpha = 0.85f)
                } else {
                    Color.White.copy(alpha = 0.85f)
                },
        )
    }
}
