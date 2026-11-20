package restarhalf.stellar.schedule.pictureselector

import androidx.compose.ui.graphics.ImageBitmap

const val IosAssetUriPrefix = "ios-asset://"

data class ImageSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "width must be > 0" }
        require(height > 0) { "height must be > 0" }
    }
}

data class CropRequest(
    val sourceUri: String,
    val sourceWidthPx: Int,
    val sourceHeightPx: Int,
    val outputWidthPx: Int,
    val outputHeightPx: Int,
    val sourceCropLeftPx: Int,
    val sourceCropTopPx: Int,
    val sourceCropWidthPx: Int,
    val sourceCropHeightPx: Int,
    val jpegQuality: Int = 90,
)

data class CropPreview(
    val sourceSize: ImageSize,
    val bitmap: ImageBitmap,
)

interface PictureSelectorPort {
    fun loadRecentImages(limit: Int, offset: Int): List<MediaImage>

    fun loadAlbums(maxScan: Int): List<MediaAlbum>

    fun loadAlbumImages(
        bucketId: Long,
        limit: Int,
        offset: Int,
    ): List<MediaImage>

    fun getImageSize(uriString: String): ImageSize?

    fun loadThumbnail(
        uriString: String,
        maxSidePx: Int,
    ): ImageBitmap?

    fun loadCropPreview(
        uriString: String,
        maxSidePx: Int,
    ): CropPreview?

    fun cropAndWriteJpegToCache(request: CropRequest): String?
}
