package restarhalf.stellar.schedule.data.wallpaper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.max

/**
 * 小部件壁纸取景：焦点（归一化 0~1）+ 缩放倍率（相对「铺满」）。
 *
 * 存的是**语义**而不是像素，所以换到任何尺寸的容器都能现算出正确的裁剪区 ——
 * 组件高度变了壁纸会自动重新取景，不会被拉伸走样。
 */
data class WidgetWallpaperCrop(
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val scale: Float = 1f,
)

/**
 * 把 [bitmapW]×[bitmapH] 的壁纸画进 [containerW]×[containerH] 时的目标矩形。
 *
 * 铺满（cover）打底，再按 [WidgetWallpaperCrop.scale] 放大、把焦点对到容器中心；
 * 偏移会被夹住，保证任何时候都不露底。
 */
fun calculateWidgetCropRect(
    bitmapW: Int,
    bitmapH: Int,
    containerW: Float,
    containerH: Float,
    crop: WidgetWallpaperCrop,
): Rect {
    if (bitmapW <= 0 || bitmapH <= 0 || containerW <= 0f || containerH <= 0f) return Rect.Zero
    val baseScale = max(containerW / bitmapW.toFloat(), containerH / bitmapH.toFloat())
    val finalScale = baseScale * crop.scale.coerceAtLeast(1f)
    val drawnWidth = bitmapW * finalScale
    val drawnHeight = bitmapH * finalScale
    val focusX = bitmapW * crop.centerX.coerceIn(0f, 1f) * finalScale
    val focusY = bitmapH * crop.centerY.coerceIn(0f, 1f) * finalScale
    val left = clampOffset(containerW / 2f - focusX, containerW, drawnWidth)
    val top = clampOffset(containerH / 2f - focusY, containerH, drawnHeight)
    return Rect(left, top, left + drawnWidth, top + drawnHeight)
}

/** 把焦点夹回合法范围：容器比例变了以后，原来的焦点可能已经越界。 */
fun clampWidgetCrop(
    crop: WidgetWallpaperCrop,
    bitmapW: Int,
    bitmapH: Int,
    containerW: Float,
    containerH: Float,
): WidgetWallpaperCrop {
    if (bitmapW <= 0 || bitmapH <= 0 || containerW <= 0f || containerH <= 0f) {
        return WidgetWallpaperCrop(
            centerX = crop.centerX.coerceIn(0f, 1f),
            centerY = crop.centerY.coerceIn(0f, 1f),
            scale = crop.scale.coerceIn(1f, 6f),
        )
    }
    val userScale = crop.scale.coerceIn(1f, 6f)
    val baseScale = max(containerW / bitmapW.toFloat(), containerH / bitmapH.toFloat())
    val finalScale = baseScale * userScale
    val minX = if (bitmapW * finalScale <= containerW) 0.5f
    else containerW / (2f * bitmapW * finalScale)
    val minY = if (bitmapH * finalScale <= containerH) 0.5f
    else containerH / (2f * bitmapH * finalScale)
    return WidgetWallpaperCrop(
        centerX = crop.centerX.coerceIn(minX, 1f - minX),
        centerY = crop.centerY.coerceIn(minY, 1f - minY),
        scale = userScale,
    )
}

/** 手势：拖动 + 缩放 → 新的取景状态（[pan] 是容器像素）。 */
fun updateWidgetCropForGesture(
    crop: WidgetWallpaperCrop,
    pan: Offset,
    zoom: Float,
    bitmapW: Int,
    bitmapH: Int,
    containerW: Float,
    containerH: Float,
): WidgetWallpaperCrop {
    if (bitmapW <= 0 || bitmapH <= 0 || containerW <= 0f || containerH <= 0f) return crop
    val nextScale = (crop.scale.coerceAtLeast(1f) * zoom).coerceIn(1f, 6f)
    val baseScale = max(containerW / bitmapW.toFloat(), containerH / bitmapH.toFloat())
    val finalScale = baseScale * nextScale
    return clampWidgetCrop(
        crop.copy(
            centerX = crop.centerX - pan.x / (bitmapW * finalScale),
            centerY = crop.centerY - pan.y / (bitmapH * finalScale),
            scale = nextScale,
        ),
        bitmapW, bitmapH, containerW, containerH,
    )
}

private fun clampOffset(offset: Float, container: Float, drawn: Float): Float {
    if (drawn <= container) return (container - drawn) / 2f
    return offset.coerceIn(container - drawn, 0f)
}
