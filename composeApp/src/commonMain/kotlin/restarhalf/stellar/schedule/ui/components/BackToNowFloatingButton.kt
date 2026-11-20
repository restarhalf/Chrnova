package restarhalf.stellar.schedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.haooz.softglass.GlassPreset
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.backdrop.effects.runtimeShaderEffect
import com.haooz.softglass.glass.BrightTone
import com.haooz.softglass.glass.CustomizeFill
import com.haooz.softglass.glass.applySoftGlass
import com.haooz.softglass.shape.ContinuousCapsule
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import restarhalf.stellar.schedule.ui.components.animation.InteractiveHighlight
import restarhalf.stellar.schedule.ui.modifier.softGlassRim
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.internal.drawBlurredPathShadow
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 「回到今日」悬浮钮的 shader 缓存键。 */
private const val BACK_TO_NOW_GLASS_KEY = "ChrnovaSoftGlassBackToNow"

@Composable
fun BackToNowFloatingButton(
    onClick: () -> Unit,
    backdrop: GlassBackdrop,
    label: String,
    modifier: Modifier = Modifier,
    horizontalTextPadding: Dp = 30.dp,
    shadowPadding: Dp = 12.dp,
) {
    val animationScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val isLightTheme = MiuixTheme.colorScheme.background.luminance() >= 0.5f
    val resolvedContainerColor =
        MiuixTheme.colorScheme.primary.copy(
            alpha =
                AppMaterialSettings.solidSurfaceAlpha(
                    if (isLightTheme) 0.72f else 0.62f,
                ),
        )

    val interactiveHighlight =
        remember(animationScope) {
            InteractiveHighlight(animationScope = animationScope)
        }

    val shadowColor = MiuixTheme.colorScheme.primary
    val shadowAlpha = 0.4f
    val interactionSource = remember { MutableInteractionSource() }
    val buttonShape: Shape = ContinuousCapsule()
    val buttonShapeBlock: () -> Shape = remember { { buttonShape } }
    val glassEffectsOn = AppMaterialSettings.glassEffectsEnabled()
    val glassBlur = AppMaterialSettings.glassBlurDp()
    val glassStyle =
        AppMaterialSettings.chromeGlassStyle(
            base = GlassPreset.Default.style,
            isDark = !isLightTheme,
            fill = CustomizeFill(MiuixTheme.colorScheme.primary),
            tone = BrightTone,
        )
    val buttonEffects: GlassEffectScope.() -> Unit =
        remember(glassEffectsOn, glassBlur, glassStyle) {
            {
                if (glassEffectsOn) {
                    blur(glassBlur.dp.toPx(), TileMode.Mirror)
                    padding = 0f
                    runtimeShaderEffect(
                        key = BACK_TO_NOW_GLASS_KEY,
                        shaderString = EDGE_SAFE_SOFT_GLASS_LIVE_SHADER,
                        uniformShaderName = "content",
                    ) {
                        applySoftGlass(style = glassStyle, width = size.width, height = size.height)
                    }
                }
            }
        }
    val buttonOnDrawSurface: DrawScope.() -> Unit =
        remember(resolvedContainerColor, interactiveHighlight) {
            {
                drawRect(resolvedContainerColor)
                drawRect(Color.Black.copy(alpha = 0.03f * interactiveHighlight.pressProgress))
            }
        }

    Box(
        modifier =
            modifier
                .wrapContentSize()
                .graphicsLayer {
                    clip = false
                    val offset = interactiveHighlight.offset
                    val contentMin = (size.minDimension - shadowPadding.toPx() * 2).coerceAtLeast(1f)
                    translationX = contentMin * tanh(0.08f * offset.x / contentMin)
                    translationY = contentMin * tanh(0.08f * offset.y / contentMin)
                }
                .drawBehind {
                    val inset = shadowPadding.toPx()
                    val offsetY = 4.dp.toPx()
                    if (size.width <= inset * 2 || size.height <= inset * 2) return@drawBehind
                    val blurRadius = 10f * density
                    val radius = (size.height - inset * 2) / 2f
                    val path =
                        Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect =
                                        Rect(
                                            inset,
                                            inset + offsetY,
                                            size.width - inset,
                                            size.height - inset + offsetY,
                                        ),
                                    cornerRadius = CornerRadius(radius, radius),
                                ),
                            )
                        }
                    drawBlurredPathShadow(
                        path = path,
                        color = shadowColor.copy(alpha = shadowAlpha),
                        blurRadius = blurRadius.coerceAtLeast(0.1f),
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(shadowPadding)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                            onClick()
                        },
                    )
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = buttonShapeBlock,
                        effects = buttonEffects,
                        shadow = null,
                        layerBlock = {
                            val width = size.width
                            val height = size.height
                            val progress = interactiveHighlight.pressProgress.coerceAtLeast(0f)
                            val scale = lerp(1f, 1f + 4f.dp.toPx() / height, progress)
                            scaleX = scale
                            scaleY = scale
                            val offset = interactiveHighlight.offset
                            val maxDragScale = 1f.dp.toPx() / height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale +
                                    maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                    (width / height).fastCoerceAtMost(1f)
                            scaleY =
                                scale +
                                    maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                    (height / width).fastCoerceAtMost(1f)
                        },
                        // 玻璃开启时柔光 shader 自己出玻璃色；无材质档才叠实色底
                        onDrawSurface = if (glassEffectsOn) null else buttonOnDrawSurface,
                    )
                    .softGlassRim(buttonShape, isLightTheme)
                    .then(interactiveHighlight.modifier)
                    .then(interactiveHighlight.gestureModifier)
                    .padding(horizontal = horizontalTextPadding)
                    .padding(vertical = 8.dp)
                    .zIndex(0f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
        }
    }
}
