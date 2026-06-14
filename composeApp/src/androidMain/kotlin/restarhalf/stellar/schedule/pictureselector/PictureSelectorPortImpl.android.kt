package restarhalf.stellar.schedule.pictureselector

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import restarhalf.stellar.schedule.core.log.AppLogger
import java.io.InputStream

class PictureSelectorPortImpl(
    private val context: Context,
) : PictureSelectorPort {
    private val contentResolver: ContentResolver = context.contentResolver

    override fun loadRecentImages(limit: Int, offset: Int): List<MediaImage> {
        val queryArgs = Bundle().apply {
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }
        return queryImages(queryArgs)
    }

    override fun loadAlbums(maxScan: Int): List<MediaAlbum> {
        val queryArgs = Bundle().apply {
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, maxScan)
        }

        val aggregation = linkedMapOf<Long, AlbumAccumulator>()
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            ),
            queryArgs,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val bucketIdIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID)
            val bucketNameIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val bucketId = cursor.getLong(bucketIdIndex)
                val bucketName = cursor.getString(bucketNameIndex).orEmpty().ifBlank { "Unknown" }
                val coverUri = buildContentUri(id)

                val accumulator = aggregation.getOrPut(bucketId) {
                    AlbumAccumulator(
                        bucketId = bucketId,
                        bucketName = bucketName,
                        coverUri = coverUri,
                        count = 0,
                    )
                }
                aggregation[bucketId] = accumulator.copy(count = accumulator.count + 1)
            }
        }

        return aggregation.values.map {
            MediaAlbum(
                bucketId = it.bucketId,
                bucketName = it.bucketName,
                coverUri = it.coverUri,
                count = it.count,
            )
        }
    }

    override fun loadAlbumImages(
        bucketId: Long,
        limit: Int,
        offset: Int,
    ): List<MediaImage> {
        val queryArgs = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Images.ImageColumns.BUCKET_ID} = ?",
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                arrayOf(bucketId.toString()),
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN),
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING,
            )
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
        }
        return queryImages(queryArgs)
    }

    override fun getImageSize(uriString: String): ImageSize? =
        decodeImageMetadata(uriString)?.size

    override fun loadThumbnail(
        uriString: String,
        maxSidePx: Int,
    ): ImageBitmap? {
        val metadata = decodeImageMetadata(uriString) ?: return null
        val sampleSize = computePreviewInSampleSize(metadata.size, maxSidePx)
        return decodeOrientedBitmap(uriString, metadata.orientation, sampleSize)?.asImageBitmap()
    }

    override fun loadCropPreview(
        uriString: String,
        maxSidePx: Int,
    ): CropPreview? {
        val metadata = decodeImageMetadata(uriString) ?: return null
        val sampleSize = computePreviewInSampleSize(metadata.size, maxSidePx)
        val bitmap =
            decodeOrientedBitmap(uriString, metadata.orientation, sampleSize) ?: return null
        return CropPreview(
            sourceSize = metadata.size,
            bitmap = bitmap.asImageBitmap(),
        )
    }

    override fun cropAndWriteJpegToCache(request: CropRequest): String? {
        val metadata = decodeImageMetadata(request.sourceUri) ?: return null
        val bitmap = decodeOrientedBitmapForCrop(request, metadata) ?: return null
        val cropped = ImageCropper.cropToRect(bitmap, request)
        return ImageCropper.writeJpegToCacheAndGetUriString(
            context = context,
            bitmap = cropped,
            quality = request.jpegQuality,
        )
    }

    private fun queryImages(queryArgs: Bundle): List<MediaImage> {
        val results = mutableListOf<MediaImage>()
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.BUCKET_ID,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            ),
            queryArgs,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateTakenIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val bucketIdIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_ID)
            val bucketNameIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                results += MediaImage(
                    id = id,
                    contentUri = buildContentUri(id),
                    dateTakenMs = cursor.getLong(dateTakenIndex),
                    bucketId = cursor.getLong(bucketIdIndex),
                    bucketName = cursor.getString(bucketNameIndex).orEmpty(),
                )
            }
        }
        return results
    }

    private fun buildContentUri(id: Long): String =
        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id).toString()

    private fun decodeImageMetadata(uriString: String): ImageMetadata? {
        val bounds = readInputStream(uriString) { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                options.outWidth to options.outHeight
            } else {
                null
            }
        } ?: return null

        val orientation = readExifOrientation(uriString)
        val size =
            if (isQuarterTurnOrientation(orientation)) {
                ImageSize(width = bounds.second, height = bounds.first)
            } else {
                ImageSize(width = bounds.first, height = bounds.second)
            }
        return ImageMetadata(size = size, orientation = orientation)
    }

    private fun decodeOrientedBitmapForCrop(
        request: CropRequest,
        metadata: ImageMetadata,
    ): Bitmap? {
        val sampleSize = computeInSampleSize(request, metadata.size)
        return decodeOrientedBitmap(request.sourceUri, metadata.orientation, sampleSize)
    }

    private fun computeInSampleSize(
        request: CropRequest,
        sourceSize: ImageSize,
    ): Int {
        val safeCropWidth = request.sourceCropWidthPx.coerceAtLeast(1)
        val safeCropHeight = request.sourceCropHeightPx.coerceAtLeast(1)
        val requiredDecodedWidth =
            (request.outputWidthPx.toFloat() * sourceSize.width / safeCropWidth).toInt()
        val requiredDecodedHeight =
            (request.outputHeightPx.toFloat() * sourceSize.height / safeCropHeight).toInt()

        return ImageCropMath.computeInSampleSize(
            sourceWidth = sourceSize.width,
            sourceHeight = sourceSize.height,
            requestedWidth = requiredDecodedWidth,
            requestedHeight = requiredDecodedHeight,
        )
    }

    private fun computePreviewInSampleSize(
        sourceSize: ImageSize,
        maxSidePx: Int,
    ): Int {
        val requested = maxSidePx.coerceAtLeast(1)
        return ImageCropMath.computeInSampleSize(
            sourceWidth = sourceSize.width,
            sourceHeight = sourceSize.height,
            requestedWidth = requested,
            requestedHeight = requested,
        )
    }

    private fun decodeOrientedBitmap(
        uriString: String,
        orientation: Int,
        sampleSize: Int,
    ): Bitmap? {
        val decoded = readInputStream(uriString) { stream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize.coerceAtLeast(1)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null

        return applyExifOrientation(decoded, orientation)
    }

    private fun readExifOrientation(uriString: String): Int =
        readInputStream(uriString) { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

    private fun isQuarterTurnOrientation(orientation: Int): Boolean =
        orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                orientation == ExifInterface.ORIENTATION_TRANSVERSE

    private fun applyExifOrientation(
        source: Bitmap,
        orientation: Int,
    ): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL ||
            orientation == ExifInterface.ORIENTATION_UNDEFINED
        ) {
            return source
        }

        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postRotate(90f)
                    postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postRotate(270f)
                    postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }

        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun <T> readInputStream(
        uriString: String,
        block: (InputStream) -> T?,
    ): T? = runCatching {
        contentResolver.openInputStream(uriString.toUri())?.use(block)
    }.onFailure {
        AppLogger.log("Picture", "读取图片输入流失败", it)
    }.getOrNull()
}

private data class AlbumAccumulator(
    val bucketId: Long,
    val bucketName: String,
    val coverUri: String,
    val count: Int,
)

private data class ImageMetadata(
    val size: ImageSize,
    val orientation: Int,
)
