@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package restarhalf.stellar.schedule.data.wallpaper

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.UIKit.UIScreen
import platform.posix.memcpy

internal class IosWallpaperBackend : WallpaperBackend {

    override suspend fun decode(bytes: ByteArray): ImageBitmap? {
        if (bytes.isEmpty()) return null
        return try {
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun decodeUri(uri: String): ImageBitmap? {
        val path = uri.removePrefix("file://")
        return try {
            val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
            Image.makeFromEncoded(data.toByteArray()).toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun loadNow(): ImageBitmap? {
        val path = try {
            restarhalf.stellar.schedule.data.platform.AppFiles.root
                .resolve(WallpaperStore.FILE_NAME)
                .path
        } catch (_: Exception) {
            return null
        }
        return try {
            val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
            Image.makeFromEncoded(data.toByteArray()).toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun encode(image: ImageBitmap, quality: Int): ByteArray? {
        val w = image.width
        val h = image.height
        if (w <= 0 || h <= 0) return null
        return try {
            val skiaBitmap = readToSkiaBitmap(image, w, h) ?: return null
            Image.makeFromBitmap(skiaBitmap)
                .encodeToData(EncodedImageFormat.JPEG, quality.coerceIn(1, 100))
                ?.bytes
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun scale(
        image: ImageBitmap,
        width: Int,
        height: Int,
    ): ImageBitmap {
        if (width <= 0 || height <= 0) return image
        if (image.width == width && image.height == height) return image
        return try {
            val src = readToSkiaBitmap(image, image.width, image.height) ?: return image
            val dst = Bitmap().apply { allocN32Pixels(width, height) }
            Canvas(dst).drawImageRect(
                image = Image.makeFromBitmap(src),
                src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                dst = Rect.makeWH(width.toFloat(), height.toFloat()),
            )
            Image.makeFromBitmap(dst).toComposeImageBitmap()
        } catch (_: Exception) {
            image
        }
    }

    override fun screenBoundsPx(): Pair<Int, Int> {
        val bounds = UIScreen.mainScreen.bounds
        val scale = UIScreen.mainScreen.scale
        var w = 0
        var h = 0
        bounds.useContents {
            w = (size.width * scale).toInt()
            h = (size.height * scale).toInt()
        }
        return w to h
    }

    private fun readToSkiaBitmap(image: ImageBitmap, w: Int, h: Int): Bitmap? {
        val pixels = IntArray(w * h)
        image.readPixels(
            buffer = pixels,
            startX = 0,
            startY = 0,
            width = w,
            height = h,
            bufferOffset = 0,
            stride = w,
        )
        val bytes = ByteArray(w * h * 4)
        for (i in pixels.indices) {
            val p = pixels[i]
            bytes[i * 4] = (p and 0xFF).toByte()
            bytes[i * 4 + 1] = ((p shr 8) and 0xFF).toByte()
            bytes[i * 4 + 2] = ((p shr 16) and 0xFF).toByte()
            bytes[i * 4 + 3] = ((p shr 24) and 0xFF).toByte()
        }
        return Bitmap().apply {
            allocN32Pixels(w, h)
            installPixels(bytes)
        }
    }
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size <= 0) return ByteArray(0)
    val sourceBytes = bytes ?: return ByteArray(size)
    return ByteArray(size).also { buffer ->
        buffer.usePinned { pinned ->
            memcpy(pinned.addressOf(0), sourceBytes, length)
        }
    }
}
