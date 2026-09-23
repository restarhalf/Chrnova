package restarhalf.stellar.schedule.core.image

import androidx.compose.ui.graphics.ImageBitmap

/** 将 [ImageBitmap] 编码为 JPEG 字节。比 PNG 快一个数量级，适合分享图落盘。 */
expect fun encodeImageBitmapToJpeg(bitmap: ImageBitmap, quality: Int = 92): ByteArray
