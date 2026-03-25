@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package restarhalf.stellar.schedule.pictureselector

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAsset
import platform.Photos.PHAssetCollection
import platform.Photos.PHAssetCollectionSubtypeAny
import platform.Photos.PHAssetCollectionTypeAlbum
import platform.Photos.PHAssetCollectionTypeSmartAlbum
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHFetchResult
import platform.Photos.PHImageContentModeAspectFit
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.max

class PictureSelectorPortImpl : PictureSelectorPort {
    override fun loadRecentImages(limit: Int, offset: Int): List<MediaImage> =
        fetchAllImageAssets()
            .sortedByDescending { it.creationDateMillis() }
            .drop(offset.coerceAtLeast(0))
            .take(limit.coerceAtLeast(0))
            .map { asset ->
                MediaImage(
                    id = asset.localIdentifier.hashCode().toLong(),
                    contentUri = assetUri(asset.localIdentifier),
                    dateTakenMs = asset.creationDateMillis(),
                    bucketId = 0L,
                    bucketName = "全部图片",
                )
            }

    override fun loadAlbums(maxScan: Int): List<MediaAlbum> =
        fetchAlbumCollections()
            .mapNotNull { collection ->
                val assets =
                    fetchAssetsInCollection(collection).sortedByDescending { it.creationDateMillis() }
                val cover = assets.firstOrNull() ?: return@mapNotNull null
                MediaAlbum(
                    bucketId = bucketIdFor(collection.localIdentifier),
                    bucketName = collection.localizedTitle ?: "未命名相册",
                    coverUri = assetUri(cover.localIdentifier),
                    count = assets.size,
                )
            }

    override fun loadAlbumImages(bucketId: Long, limit: Int, offset: Int): List<MediaImage> {
        val collection = fetchAlbumCollections().firstOrNull {
            bucketIdFor(it.localIdentifier) == bucketId
        } ?: return emptyList()
        val bucketName = collection.localizedTitle ?: "未命名相册"
        return fetchAssetsInCollection(collection)
            .sortedByDescending { it.creationDateMillis() }
            .drop(offset.coerceAtLeast(0))
            .take(limit.coerceAtLeast(0))
            .map { asset ->
                MediaImage(
                    id = asset.localIdentifier.hashCode().toLong(),
                    contentUri = assetUri(asset.localIdentifier),
                    dateTakenMs = asset.creationDateMillis(),
                    bucketId = bucketId,
                    bucketName = bucketName,
                )
            }
    }

    override fun getImageSize(uriString: String): ImageSize? =
        loadSourceImage(uriString)?.toPixelSize()

    override fun loadThumbnail(
        uriString: String,
        maxSidePx: Int,
    ): ImageBitmap? =
        when {
            uriString.startsWith(IosAssetUriPrefix) -> {
                val asset = resolveAsset(uriString) ?: return null
                requestAssetImage(asset, maxSidePx.coerceAtLeast(1))?.toComposeBitmap()
            }

            else -> loadLocalImage(uriString)?.scaleDownIfNeeded(maxSidePx.coerceAtLeast(1))
                ?.toComposeBitmap()
        }

    override fun loadCropPreview(
        uriString: String,
        maxSidePx: Int,
    ): CropPreview? {
        val image = loadSourceImage(uriString) ?: return null
        val sourceSize = image.toPixelSize() ?: return null
        val preview = image.scaleDownIfNeeded(maxSidePx.coerceAtLeast(1)) ?: return null
        return CropPreview(
            sourceSize = sourceSize,
            bitmap = preview.toComposeBitmap() ?: return null,
        )
    }

    override fun cropAndWriteJpegToCache(request: CropRequest): String? {
        val image = loadSourceImage(request.sourceUri) ?: return null
        val sourceSize = image.toPixelSize() ?: return null
        val cropRect = ImageCropMath.resolveBitmapCropRect(
            sourceWidth = sourceSize.width,
            sourceHeight = sourceSize.height,
            request = request,
        )
        val rendered = renderCroppedImage(
            image = image,
            sourceSize = sourceSize,
            cropRect = cropRect,
            outputWidthPx = request.outputWidthPx.coerceAtLeast(1),
            outputHeightPx = request.outputHeightPx.coerceAtLeast(1),
        ) ?: return null
        return writeTempJpeg(rendered, "crop")
    }
}

private fun fetchAllImageAssets(): List<PHAsset> =
    fetchAssets(PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, null))

private fun fetchAlbumCollections(): List<PHAssetCollection> {
    val collections = mutableListOf<PHAssetCollection>()
    collections += fetchCollections(
        PHAssetCollection.fetchAssetCollectionsWithType(
            PHAssetCollectionTypeSmartAlbum,
            PHAssetCollectionSubtypeAny,
            null,
        ),
    )
    collections += fetchCollections(
        PHAssetCollection.fetchAssetCollectionsWithType(
            PHAssetCollectionTypeAlbum,
            PHAssetCollectionSubtypeAny,
            null,
        ),
    )
    return collections
        .distinctBy { it.localIdentifier }
        .filter { fetchAssetsInCollection(it).isNotEmpty() }
}

private fun fetchAssetsInCollection(collection: PHAssetCollection): List<PHAsset> =
    fetchAssets(PHAsset.fetchAssetsInAssetCollection(collection, null))

private fun fetchAssets(result: PHFetchResult): List<PHAsset> = buildList {
    result.enumerateObjectsUsingBlock { item, _, _ ->
        val asset = item as? PHAsset ?: return@enumerateObjectsUsingBlock
        add(asset)
    }
}

private fun fetchCollections(result: PHFetchResult): List<PHAssetCollection> = buildList {
    result.enumerateObjectsUsingBlock { item, _, _ ->
        val collection = item as? PHAssetCollection ?: return@enumerateObjectsUsingBlock
        add(collection)
    }
}

private fun resolveAsset(uriString: String): PHAsset? {
    val identifier = uriString.removePrefix(IosAssetUriPrefix)
    if (identifier == uriString) return null
    val result = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(identifier), null)
    return fetchAssets(result).firstOrNull()
}

private fun assetUri(localIdentifier: String): String = "$IosAssetUriPrefix$localIdentifier"

private fun bucketIdFor(localIdentifier: String): Long = localIdentifier.hashCode().toLong()

private fun PHAsset.creationDateMillis(): Long =
    ((creationDate?.timeIntervalSince1970 ?: 0.0) * 1000.0).toLong()

private fun loadSourceImage(uriString: String): UIImage? =
    when {
        uriString.startsWith(IosAssetUriPrefix) -> {
            val asset = resolveAsset(uriString) ?: return null
            requestAssetImage(asset, null)
        }

        else -> loadLocalImage(uriString)
    }

private fun loadLocalImage(path: String): UIImage? {
    val data = NSData.dataWithContentsOfURL(localFileUrl(path)) ?: return null
    val image = UIImage(data = data)
    return normalizeUIImage(image)
}

private fun requestAssetImage(
    asset: PHAsset,
    maxSidePx: Int?,
): UIImage? {
    val assetWidth = asset.pixelWidth.toInt().coerceAtLeast(1)
    val assetHeight = asset.pixelHeight.toInt().coerceAtLeast(1)
    val targetSize =
        if (maxSidePx == null) {
            CGSizeMake(assetWidth.toDouble(), assetHeight.toDouble())
        } else {
            val scale = maxSidePx.toDouble() / max(assetWidth, assetHeight).toDouble()
            val safeScale = if (scale > 1.0) 1.0 else scale
            CGSizeMake(
                (assetWidth * safeScale).coerceAtLeast(1.0),
                (assetHeight * safeScale).coerceAtLeast(1.0),
            )
        }
    val options = PHImageRequestOptions().apply {
        synchronous = true
        networkAccessAllowed = true
    }
    var image: UIImage? = null
    PHImageManager.defaultManager().requestImageForAsset(
        asset,
        targetSize,
        PHImageContentModeAspectFit,
        options,
    ) { result, _ ->
        image = result
    }
    return image?.let(::normalizeUIImage)
}

private fun UIImage.toPixelSize(): ImageSize? {
    val pointSize = size
    val width = (pointSize.useContents { width } * scale).toInt().coerceAtLeast(1)
    val height = (pointSize.useContents { height } * scale).toInt().coerceAtLeast(1)
    return if (width > 0 && height > 0) {
        ImageSize(width = width, height = height)
    } else {
        null
    }
}

private fun UIImage.toComposeBitmap(): ImageBitmap? {
    val data = UIImageJPEGRepresentation(this, 0.95) ?: return null
    return runCatching {
        Image.makeFromEncoded(data.toByteArray()).toComposeImageBitmap()
    }.getOrNull()
}

private fun UIImage.scaleDownIfNeeded(maxSidePx: Int): UIImage? {
    val pixelSize = toPixelSize() ?: return null
    val longestSide = maxOf(pixelSize.width, pixelSize.height)
    if (longestSide <= maxSidePx) return this
    val scale = maxSidePx.toDouble() / longestSide.toDouble()
    return scaleTo(
        widthPx = (pixelSize.width * scale).toInt().coerceAtLeast(1),
        heightPx = (pixelSize.height * scale).toInt().coerceAtLeast(1),
    )
}

private fun UIImage.scaleTo(
    widthPx: Int,
    heightPx: Int,
): UIImage? {
    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(widthPx.toDouble(), heightPx.toDouble()),
        false,
        1.0,
    )
    return try {
        drawInRect(
            CGRectMake(
                0.0,
                0.0,
                widthPx.toDouble(),
                heightPx.toDouble(),
            ),
        )
        UIGraphicsGetImageFromCurrentImageContext()
    } finally {
        UIGraphicsEndImageContext()
    }
}

private fun renderCroppedImage(
    image: UIImage,
    sourceSize: ImageSize,
    cropRect: BitmapCropRect,
    outputWidthPx: Int,
    outputHeightPx: Int,
): UIImage? {
    val scaleX = outputWidthPx.toDouble() / cropRect.width.toDouble()
    val scaleY = outputHeightPx.toDouble() / cropRect.height.toDouble()
    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(outputWidthPx.toDouble(), outputHeightPx.toDouble()),
        false,
        1.0,
    )
    return try {
        image.drawInRect(
            CGRectMake(
                -cropRect.left * scaleX,
                -cropRect.top * scaleY,
                sourceSize.width * scaleX,
                sourceSize.height * scaleY,
            ),
        )
        UIGraphicsGetImageFromCurrentImageContext()
    } finally {
        UIGraphicsEndImageContext()
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
