package restarhalf.stellar.schedule.papers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypePDF
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Composable
fun PdfFilePickerHost(
    onPicked: (ByteArray, String, String) -> Unit,
) {
    LaunchedEffect(Unit) {
        val controller = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypePDF),
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
        onPicked(bytes, fileName, "application/pdf")
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
    }
}
