package top.yukonga.miuix.kmp.overlay

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/** Fades only this layer, leaving the sheet background and toolbar intact. */
internal fun Modifier.sheetTopOpacityMask(
    height: Dp,
    strength: () -> Float,
): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithCache {
    val maskHeight = height.toPx()
    // Match the original overlay's piecewise linear fade using alpha removal.
    val stops = arrayOf(
        0f to Color.White.copy(alpha = 0.85f),
        0.45f to Color.White.copy(alpha = 0.55f),
        0.7f to Color.White.copy(alpha = 0.32f),
        0.85f to Color.White.copy(alpha = 0.14f),
        0.93f to Color.White.copy(alpha = 0.05f),
        1f to Color.Transparent,
    )
    val brush = Brush.verticalGradient(
        colorStops = stops,
        startY = 0f,
        endY = maskHeight,
    )
    onDrawWithContent {
        drawContent()
        val progress = strength().coerceIn(0f, 1f)
        if (progress > 0f) {
            // DstOut gives finalAlpha = originalAlpha * (1 - progress * (1 - opacity)).
            drawRect(
                brush = brush,
                size = Size(size.width, maskHeight),
                alpha = progress,
                blendMode = BlendMode.DstOut,
            )
        }
    }
}
