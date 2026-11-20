package restarhalf.stellar.schedule.ui.components.topbar

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import com.haooz.softglass.backdrop.GlassBackdrop
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import restarhalf.stellar.schedule.ui.components.animation.InteractiveHighlight
import restarhalf.stellar.schedule.ui.theme.AppMaterialSettings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LiquidTopBarButton(
    onClick: () -> Unit,
    backdrop: GlassBackdrop?,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 23.dp,
    buttonSize: Dp = 42.dp,
    backdropAlpha: Float = 1f,
    shadowAlpha: Float = 1f,
    containerColor: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val animationScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isLightTheme = MiuixTheme.colorScheme.background.luminance() >= 0.5f
    val highlight =
        remember(animationScope, isLightTheme) {
            InteractiveHighlight(animationScope, isDark = !isLightTheme)
        }

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
        LiquidGlassCircle(
            backdrop = backdrop,
            size = buttonSize,
            backdropAlpha = backdropAlpha,
            shadowAlpha = shadowAlpha,
            containerColor = resolvedContainerColor,
        )
        Box(
            modifier =
                Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .then(highlight.modifier)
                    .then(if (enabled) highlight.gestureModifier else Modifier)
                    .clickable(
                        enabled = enabled,
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
