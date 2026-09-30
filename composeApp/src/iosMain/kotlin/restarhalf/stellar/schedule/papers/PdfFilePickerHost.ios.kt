package restarhalf.stellar.schedule.papers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTagClassFilenameExtension
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.typeWithTag
import platform.darwin.NSObject
import platform.posix.memcpy

/**
 * 文档/图片选择器宿主（iOS）。
 *
 * 选择结束（含取消）必定回调 [onPicked]；delegate 需强持有，避免系统回收导致只弹一次。
 *
 * @param multiple 是否允许多选
 * @param onPicked 选择完成后一次性回调全部文件（取消为空列表）
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
fun PdfFilePickerHost(
    multiple: Boolean = false,
    onPicked: (List<PickedAttachment>) -> Unit,
) {
    // 强持有 delegate：UIDocumentPickerViewController.delegate 不会 retain
    val delegateHolder = remember { arrayOfNulls<PdfPickerDelegate>(1) }

    LaunchedEffect(Unit) {
        val docType = UTType.typeWithTag("doc", UTTagClassFilenameExtension, null)
        val docxType = UTType.typeWithTag("docx", UTTagClassFilenameExtension, null)
        val types = listOfNotNull(
            UTTypePDF,
            UTTypeImage,
            docType,
            docxType,
        )
        val controller = UIDocumentPickerViewController(
            forOpeningContentTypes = types,
            asCopy = true,
        )
        controller.allowsMultipleSelection = multiple

        val delegate = PdfPickerDelegate(onPicked)
        delegateHolder[0] = delegate
        controller.delegate = delegate

        val rootController = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
        rootController?.presentViewController(controller, animated = true, completion = null)
    }
}

private class PdfPickerDelegate(
    private val onPicked: (List<PickedAttachment>) -> Unit,
) : NSObject(), platform.UIKit.UIDocumentPickerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val picked = didPickDocumentsAtURLs.mapNotNull { item ->
            val url = item as? NSURL ?: return@mapNotNull null
            val data: NSData = NSData.dataWithContentsOfURL(url) ?: return@mapNotNull null
            val length = data.length.toInt()
            if (length <= 0) return@mapNotNull null
            val bytes = ByteArray(length)
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
            val fileName = url.lastPathComponent ?: "document.pdf"
            PickedAttachment(bytes = bytes, name = fileName, mime = guessMimeFromName(fileName))
        }
        onPicked(picked)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onPicked(emptyList())
    }
}

private fun guessMimeFromName(name: String): String = when {
    name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
    name.endsWith(".doc", ignoreCase = true) -> "application/msword"
    name.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
    name.endsWith(".png", ignoreCase = true) -> "image/png"
    else -> "application/octet-stream"
}
