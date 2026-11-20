package restarhalf.stellar.schedule.ui.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.haooz.softglass.GlassPreset
import com.haooz.softglass.backdrop.GlassBackdrop
import com.haooz.softglass.backdrop.GlassEffectScope
import com.haooz.softglass.backdrop.drawBackdrop
import com.haooz.softglass.backdrop.effects.blur
import com.haooz.softglass.backdrop.effects.runtimeShaderEffect
import com.haooz.softglass.glass.BrightTone
import com.haooz.softglass.glass.ThemeFill
import com.haooz.softglass.glass.applySoftGlass
import restarhalf.stellar.schedule.ui.components.EDGE_SAFE_SOFT_GLASS_LIVE_SHADER
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import restarhalf.stellar.schedule.ui.modifier.softGlassRim
import top.yukonga.miuix.kmp.internal.drawBlurredPathShadow
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 顶栏圆形玻璃钮的 shader 缓存键。 */
private const val CIRCLE_GLASS_KEY = "{{SECRET_a5rqzw8n}}"

@Composable
fun LiquidGlassCircle(
    backdrop: GlassBackdrop?,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    backdropAlpha: Float = 1f,
    shadowAlpha: Float = 1f,
    containerColor: Color = Color.Unspecified,
) {
    val isLightTheme = MiuixTheme.colorScheme.background.luminance() >= 0.5f
    val resolvedContainerColor =
        if (containerColor != Color.Unspecified) {
            containerColor
        } else if (MiuixTheme.isDynamicColor) {
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
    val shadowColor = if (isLightTheme) Color(0x12000000) else Color(0x20000000)
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
                    // 这里的 size 是 GlassEffectScope 的绘制尺寸；函数参数里也有同名的 Dp，必须显式取 receiver
                    val w = this.size.width
                    val h = this.size.height
                    blur(glassBlur.dp.toPx(), TileMode.Mirror)
                    padding = 0f
                    runtimeShaderEffect(
                        key = CIRCLE_GLASS_KEY,
                        shaderString = EDGE_SAFE_SOFT_GLASS_LIVE_SHADER,
                        uniformShaderName = "content",
                    ) {
                        applySoftGlass(style = glassStyle, width = w, height = h)
                    }
                }
            }
        }
    val surface: DrawScope.() -> Unit =
        remember(resolvedContainerColor) { { drawRect(resolvedContainerColor) } }

    Box(
        modifier =
            modifier
                .size(size)
                .drawBehind {
                    val spread = shadowAlpha
                    if (spread > 0.01f) {
                        val maxBlurRadius = 10f * density
                        val maxShadowSpread = 2f * density
                        val blurRadius = maxBlurRadius * spread
                        val shadowSpread = maxShadowSpread * spread
                        val outerRadius = this.size.minDimension / 2f + shadowSpread
                        val innerRadius = this.size.minDimension / 2f
                        val path =
                            Path().apply {
                                addOval(
                                    Rect(
                                        center.x - outerRadius,
                                        center.y - outerRadius,
                                        center.x + outerRadius,
                                        center.y + outerRadius,
                                    ),
                                    Path.Direction.Clockwise,
                                )
                                addOval(
                                    Rect(
                                        center.x - innerRadius,
                                        center.y - innerRadius,
                                        center.x + innerRadius,
                                        center.y + innerRadius,
                                    ),
                                    Path.Direction.CounterClockwise,
                                )
                            }
                        drawBlurredPathShadow(
                            path = path,
                            color = shadowColor.copy(alpha = (shadowColor.alpha * 3.2f).coerceAtMost(1f)),
                            blurRadius = blurRadius.coerceAtLeast(0.1f),
                        )
                    }
                },
    ) {
        if (backdrop != null) {
            Box(
                modifier =
                    Modifier
                        .size(size)
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = shapeProvider,
                            effects = effects,
                            shadow = null,
                            layerBlock = { alpha = backdropAlpha },
                            onDrawSurface = if (glassEffectsOn) null else surface,
                        )
                        .softGlassRim(CircleShape, isLightTheme),
            )
        } else {
            Box(modifier = Modifier.size(size).clip(CircleShape).background(resolvedContainerColor))
        }
    }
}
