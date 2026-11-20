package restarhalf.stellar.schedule.pictureselector

import kotlin.math.ceil
import kotlin.math.max

data class BitmapCropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

object ImageCropMath {
    fun resolveBitmapCropRect(
        sourceWidth: Int,
        sourceHeight: Int,
        request: CropRequest,
    ): BitmapCropRect {
        val safeOriginalWidth = request.sourceWidthPx.coerceAtLeast(1)
        val safeOriginalHeight = request.sourceHeightPx.coerceAtLeast(1)
        val safeBitmapWidth = sourceWidth.coerceAtLeast(1)
        val safeBitmapHeight = sourceHeight.coerceAtLeast(1)

        val cropLeft = request.sourceCropLeftPx.coerceIn(0, safeOriginalWidth - 1)
        val cropTop = request.sourceCropTopPx.coerceIn(0, safeOriginalHeight - 1)
        val cropWidth = request.sourceCropWidthPx.coerceAtLeast(1)
        val cropHeight = request.sourceCropHeightPx.coerceAtLeast(1)
        val cropRight = (cropLeft + cropWidth).coerceIn(cropLeft + 1, safeOriginalWidth)
        val cropBottom = (cropTop + cropHeight).coerceIn(cropTop + 1, safeOriginalHeight)

        val scaleX = safeBitmapWidth.toFloat() / safeOriginalWidth.toFloat()
        val scaleY = safeBitmapHeight.toFloat() / safeOriginalHeight.toFloat()

        val left = (cropLeft * scaleX).toInt().coerceIn(0, safeBitmapWidth - 1)
        val top = (cropTop * scaleY).toInt().coerceIn(0, safeBitmapHeight - 1)
        val right = ceil(cropRight * scaleX).toInt().coerceIn(left + 1, safeBitmapWidth)
        val bottom = ceil(cropBottom * scaleY).toInt().coerceIn(top + 1, safeBitmapHeight)

        return BitmapCropRect(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
    }

    fun computeInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        requestedWidth: Int,
        requestedHeight: Int,
    ): Int {
        val safeRequestedWidth = requestedWidth.coerceAtLeast(1)
        val safeRequestedHeight = requestedHeight.coerceAtLeast(1)

        var inSampleSize = 1
        val nextWidth = sourceWidth / 2
        val nextHeight = sourceHeight / 2

        while (nextWidth / inSampleSize >= safeRequestedWidth &&
            nextHeight / inSampleSize >= safeRequestedHeight
        ) {
            inSampleSize *= 2
        }

        return max(1, inSampleSize)
    }
}
