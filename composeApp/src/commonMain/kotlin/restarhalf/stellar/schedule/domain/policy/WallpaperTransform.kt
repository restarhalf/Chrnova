package restarhalf.stellar.schedule.domain.policy

import kotlin.math.max
import kotlin.math.min

object WallpaperTransform {

    fun fitScale(bitmapW: Int, bitmapH: Int, screenW: Float, screenH: Float): Float {
        if (bitmapW <= 0 || bitmapH <= 0 || screenW <= 0f || screenH <= 0f) return 1f
        return min(screenW / bitmapW, screenH / bitmapH)
    }

    fun coverScale(bitmapW: Int, bitmapH: Int, screenW: Float, screenH: Float): Float {
        if (bitmapW <= 0 || bitmapH <= 0 || screenW <= 0f || screenH <= 0f) return 1f
        return max(screenW / bitmapW, screenH / bitmapH)
    }

    fun minScale(bitmapW: Int, bitmapH: Int, screenW: Float, screenH: Float): Float {
        val fit = fitScale(bitmapW, bitmapH, screenW, screenH)
        if (fit <= 0f) return 1f
        val cover = coverScale(bitmapW, bitmapH, screenW, screenH)
        return cover / fit
    }

    fun maxOffset(
        bitmapW: Int,
        bitmapH: Int,
        screenW: Float,
        screenH: Float,
        scale: Float,
    ): Pair<Float, Float> {
        val fit = fitScale(bitmapW, bitmapH, screenW, screenH)
        val scaledW = bitmapW * fit * scale
        val scaledH = bitmapH * fit * scale
        return ((scaledW - screenW) / 2f).coerceAtLeast(0f) to
            ((scaledH - screenH) / 2f).coerceAtLeast(0f)
    }
}
