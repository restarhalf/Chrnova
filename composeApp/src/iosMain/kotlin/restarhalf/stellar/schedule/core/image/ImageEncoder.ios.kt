package restarhalf.stellar.schedule.core.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun encodeImageBitmapToJpeg(bitmap: ImageBitmap, quality: Int): ByteArray {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return ByteArray(0)

    // 整块打包像素，避免逐点再拆一次 Color
    val pixels = bitmap.toPixelMap()
    val rgba = ByteArray(width * height * 4)
    var o = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val c = pixels[x, y]
            rgba[o++] = (c.red * 255f + 0.5f).toInt().toByte()
            rgba[o++] = (c.green * 255f + 0.5f).toInt().toByte()
            rgba[o++] = (c.blue * 255f + 0.5f).toInt().toByte()
            rgba[o++] = (c.alpha * 255f + 0.5f).toInt().toByte()
        }
    }

    val skBitmap = Bitmap().apply {
        allocPixels(ImageInfo.makeN32Premul(width, height))
        installPixels(rgba)
    }
    return Image.makeFromBitmap(skBitmap)
        .encodeToData(EncodedImageFormat.JPEG, quality.coerceIn(50, 100))
        ?.bytes
        ?: ByteArray(0)
}
