package restarhalf.stellar.schedule.data.wallpaper

import androidx.compose.ui.graphics.ImageBitmap

interface WallpaperBackend {
    suspend fun decode(bytes: ByteArray): ImageBitmap?

    suspend fun decodeUri(uri: String): ImageBitmap?

    suspend fun encode(image: ImageBitmap, quality: Int): ByteArray?

    suspend fun scale(image: ImageBitmap, width: Int, height: Int): ImageBitmap

    fun screenBoundsPx(): Pair<Int, Int>

    fun saveTargetSize(width: Int, height: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return width to height
        val (targetW, targetH) = screenBoundsPx()
        if (targetW <= 0 || targetH <= 0) return width to height
        val cover = maxOf(
            targetW / width.toFloat(),
            targetH / height.toFloat(),
        )
        if (cover > 1f) return width to height
        val maxLong = 2 * maxOf(targetW, targetH)
        val longSide = maxOf(width, height)
        if (longSide <= maxLong) return width to height
        val s = maxLong / longSide.toFloat()
        return (width * s).toInt().coerceAtLeast(1) to (height * s).toInt().coerceAtLeast(1)
    }
}
