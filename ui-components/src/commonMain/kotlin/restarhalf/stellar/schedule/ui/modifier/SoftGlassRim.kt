package restarhalf.stellar.schedule.ui.modifier

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 柔光玻璃外缘的纯 Compose 描边。
 *
 * 只负责轮廓高光，不采样背景；玻璃主体仍由 softglass 的 drawBackdrop 绘制。
 */
fun Modifier.softGlassRim(
    shape: Shape,
    isLightTheme: Boolean,
    width: Dp = 0.8.dp,
): Modifier = drawWithCache {
    val top = if (isLightTheme) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.34f)
    val bottom = if (isLightTheme) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f)
    val brush = Brush.linearGradient(
        colors = listOf(top, bottom),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val stroke = Stroke(width = width.toPx())
    onDrawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(
            outline = outline,
            brush = brush,
            style = stroke,
        )
    }
}
