package restarhalf.stellar.schedule.pictureselector

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Clock

object ImageCropper {
    fun cropToRect(
        source: Bitmap,
        request: CropRequest,
    ): Bitmap {
        val cropRect =
            ImageCropMath.resolveBitmapCropRect(
                sourceWidth = source.width,
                sourceHeight = source.height,
                request = request,
            )
        val cropped =
            Bitmap.createBitmap(
                source,
                cropRect.left,
                cropRect.top,
                cropRect.width,
                cropRect.height,
            )

        return if (
            cropped.width == request.outputWidthPx &&
            cropped.height == request.outputHeightPx
        ) {
            cropped
        } else {
            cropped.scale(
                request.outputWidthPx.coerceAtLeast(1),
                request.outputHeightPx.coerceAtLeast(1),
            )
        }
    }

    fun writeJpegToCacheAndGetUriString(
        context: Context,
        bitmap: Bitmap,
        quality: Int,
    ): String? {
        val cacheDir = File(context.cacheDir, "pictureselector").apply { mkdirs() }
        val outputFile =
            File(cacheDir, "crop_${Clock.System.now().toEpochMilliseconds()}.jpg")

        return runCatching {
            FileOutputStream(outputFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), output)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile,
            ).toString()
        }.getOrNull()
    }
}
