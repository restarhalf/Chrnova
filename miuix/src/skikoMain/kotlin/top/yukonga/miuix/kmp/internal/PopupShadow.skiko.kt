package top.yukonga.miuix.kmp.internal

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Skia（iOS / Desktop / wasm）实现：环形路径交给 [drawBlurredPathShadow]。
 *
 * 环形 = 外圈 CW 减内圈 CCW（nonzero winding 差集），再整体模糊 —— 与 Android 侧
 * `BlurMaskFilter` 的语义一致。
 *
 * ⚠ 底层刻意复用 [drawBlurredPathShadow] 而不是另写一套 Skia Paint：app 层
 * `LiquidGlassDropdownMenu` 的阴影就是这么画的，两处共用同一实现才能保证
 * iOS 上两种下拉菜单的阴影观感一致（参数本来就逐字同值）。
 */
actual fun DrawScope.drawBlurredRingShadow(
    size: Size,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
    spread: Float,
    blurRadius: Float,
    color: Color,
) {
    if (blurRadius <= 0f) return

    val ringPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(
                    left - spread,
                    top - spread,
                    left + width + spread,
                    top + height + spread,
                ),
                cornerRadius = CornerRadius(radius + spread, radius + spread),
            ),
            Path.Direction.Clockwise,
        )
        addRoundRect(
            RoundRect(
                rect = Rect(left, top, left + width, top + height),
                cornerRadius = CornerRadius(radius, radius),
            ),
            Path.Direction.CounterClockwise,
        )
    }

    drawBlurredPathShadow(
        path = ringPath,
        color = color,
        // Skia 的 makeBlur 不接受 0（会退化），与 Android 侧的 coerceAtLeast(0.1f) 对齐
        blurRadius = blurRadius.coerceAtLeast(0.1f),
    )
}
