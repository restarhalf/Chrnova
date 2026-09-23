package restarhalf.stellar.schedule.core.image

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun encodeImageBitmapToJpeg(bitmap: ImageBitmap, quality: Int): ByteArray {
    val stream = ByteArrayOutputStream()
    val androidBitmap = bitmap.asAndroidBitmap()
    // JPEG 远快于 PNG；分享图不需要无损
    androidBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(50, 100), stream)
    return stream.toByteArray()
}
