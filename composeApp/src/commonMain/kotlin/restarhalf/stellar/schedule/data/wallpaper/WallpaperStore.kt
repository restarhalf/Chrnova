package restarhalf.stellar.schedule.data.wallpaper

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.data.platform.AppFiles
import restarhalf.stellar.schedule.platform.AppIoDispatcher

object WallpaperStore {

    const val FILE_NAME = "appearance_wallpaper.webp"

    private const val GRID = 16

    var backend: WallpaperBackend? = null

    var synchronousLoader: (() -> ImageBitmap?)? = null

    private var cached: ImageBitmap? = null

    fun currentImageBitmap(): ImageBitmap? = cached

    fun setCached(image: ImageBitmap?) {
        cached = image
    }

    fun filePath(): String? {
        if (!AppFiles.isReady) return null
        return try {
            val file = AppFiles.root.resolve(FILE_NAME)
            if (file.exists()) file.path else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun load(): ImageBitmap? {
        cached?.let { return it }
        val be = backend ?: return null
        if (!AppFiles.isReady) return null
        val file = AppFiles.root.resolve(FILE_NAME)
        if (!file.exists()) return null
        val decoded = withContext(AppIoDispatcher) {
            val bytes = try {
                file.readBytes()
            } catch (_: Exception) {
                return@withContext null
            }
            be.decode(bytes)
        } ?: return null
        cached = decoded
        return decoded
    }

    suspend fun save(image: ImageBitmap): Boolean {
        val be = backend ?: return false
        if (!AppFiles.isReady) return false
        return try {
            withContext(AppIoDispatcher) {
                val (targetW, targetH) = be.saveTargetSize(image.width, image.height)
                val scaled = if (targetW == image.width && targetH == image.height) {
                    image
                } else {
                    be.scale(image = image, width = targetW, height = targetH)
                }
                val bytes = be.encode(scaled, quality = 80) ?: return@withContext false
                AppFiles.root.resolve(FILE_NAME).writeBytes(bytes)
                cached = scaled
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun clear() {
        cached = null
        if (!AppFiles.isReady) return
        withContext(AppIoDispatcher) {
            try {
                AppFiles.root.resolve(FILE_NAME).delete()
            } catch (_: Exception) {
            }
        }
    }

    fun measureLightness(image: ImageBitmap?): Boolean? {
        if (image == null || image.width <= 0 || image.height <= 0) return null
        return try {
            val pixels = image.toPixelMap()
            var sum = 0.0
            for (x in 0 until GRID) {
                for (y in 0 until GRID) {
                    val px = ((x + 0.5f) / GRID * image.width).toInt()
                        .coerceIn(0, image.width - 1)
                    val py = ((y + 0.5f) / GRID * image.height).toInt()
                        .coerceIn(0, image.height - 1)
                    val c = pixels[px, py]
                    sum += 0.299 * (c.red * 255f) + 0.587 * (c.green * 255f) + 0.114 * (c.blue * 255f)
                }
            }
            (sum / (GRID * GRID)) >= 128
        } catch (_: Exception) {
            null
        }
    }
}
