package restarhalf.stellar.schedule.data.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File

class AndroidWallpaperBackend(
    private val context: Context,
) : WallpaperBackend {

    override suspend fun decode(bytes: ByteArray): ImageBitmap? = try {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sampleOptions(bytes))
            ?.asImageBitmap()
    } catch (_: Exception) {
        null
    }

    override suspend fun decodeUri(uri: String): ImageBitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri.toUri())?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val options = sampleOptions(bounds.outWidth, bounds.outHeight)
        context.contentResolver.openInputStream(uri.toUri())?.use {
            BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }

    override suspend fun encode(image: ImageBitmap, quality: Int): ByteArray? = try {
        val out = ByteArrayOutputStream()
        @Suppress("DEPRECATION")
        val format =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
        val ok = image.asAndroidBitmap().compress(format, quality.coerceIn(1, 100), out)
        if (ok) out.toByteArray() else null
    } catch (_: Exception) {
        null
    }

    override suspend fun scale(
        image: ImageBitmap,
        width: Int,
        height: Int,
    ): ImageBitmap {
        if (width <= 0 || height <= 0) return image
        if (image.width == width && image.height == height) return image
        return try {
            Bitmap.createScaledBitmap(image.asAndroidBitmap(), width, height, true).asImageBitmap()
        } catch (_: Exception) {
            image
        }
    }

    override fun screenBoundsPx(): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        return metrics.widthPixels to metrics.heightPixels
    }

    fun loadNow(): ImageBitmap? = try {
        val file = File(context.filesDir, WallpaperStore.FILE_NAME)
        if (!file.exists()) {
            null
        } else {
            val bytes = file.readBytes()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sampleOptions(bytes))
                ?.asImageBitmap()
        }
    } catch (_: Exception) {
        null
    }

    private fun sampleOptions(bytes: ByteArray): BitmapFactory.Options {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return sampleOptions(bounds.outWidth, bounds.outHeight)
    }

    private fun sampleOptions(sourceWidth: Int, sourceHeight: Int): BitmapFactory.Options {
        val (targetW, targetH) = screenBoundsPx()
        return BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(sourceWidth, sourceHeight, targetW, targetH)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    }

    private fun calculateInSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Int {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) return 1
        var sample = 1
        while (sourceWidth / (sample * 2) >= targetWidth && sourceHeight / (sample * 2) >= targetHeight) {
            sample *= 2
        }
        return sample
    }
}
