package restarhalf.stellar.schedule.core.image

import androidx.compose.ui.graphics.ImageBitmap

expect fun encodeImageBitmapToJpeg(bitmap: ImageBitmap, quality: Int = 92): ByteArray
