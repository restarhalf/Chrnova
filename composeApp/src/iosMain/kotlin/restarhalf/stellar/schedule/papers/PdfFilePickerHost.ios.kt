package restarhalf.stellar.schedule.papers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCoder
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTagClassFilenameExtension
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.UniformTypeIdentifiers.typeWithTag
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
fun PdfFilePickerHost(
    onPicked: (ByteArray, String, String) -> Unit,
) {
    val docType = UTType.typeWithTag("doc", UTTagClassFilenameExtension, null)
    val docxType = UTType.typeWithTag("docx", UTTagClassFilenameExtension, null)
    LaunchedEffect(Unit) {
        val types = listOf(
            UTTypePDF,
            docType,
            docxType
        )
        val controller = UIDocumentPickerViewController(
            forOpeningContentTypes = types,
            asCopy = true,
        )

        val delegate = PdfPickerDelegate(onPicked)
        controller.delegate = delegate

        val rootController = platform.UIKit.UIApplication.sharedApplication.keyWindow?.rootViewController
        rootController?.presentViewController(controller, animated = true, completion = null)
    }
}

private class PdfPickerDelegate(
    private val onPicked: (ByteArray, String, String) -> Unit,
) : NSObject(), platform.UIKit.UIDocumentPickerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? platform.Foundation.NSURL ?: return
        val data: NSData = NSData.dataWithContentsOfURL(url) ?: return
        val length = data.length.toInt()
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        val fileName = url.lastPathComponent ?: "document.pdf"
        val mimeType = guessMimeFromName(fileName)
        onPicked(bytes, fileName, mimeType)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
    }
}

private fun guessMimeFromName(name: String): String = when {
    name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
    name.endsWith(".doc", ignoreCase = true) -> "application/msword"
    name.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    else -> "application/octet-stream"
}
