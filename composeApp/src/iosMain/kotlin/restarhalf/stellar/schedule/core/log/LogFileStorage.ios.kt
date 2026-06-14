package restarhalf.stellar.schedule.core.log

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSURL
import platform.Foundation.NSNumber
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToURL

actual object LogFileStorage {
    private var logFilePath: String? = null

    @OptIn(ExperimentalForeignApi::class)
    actual fun init(logDir: String) {
        val dirPath = NSSearchPathForDirectoriesInDomains(
            NSLibraryDirectory, NSUserDomainMask, true
        ).firstOrNull() as? String ?: return
        val logDirResolved = "$dirPath/Application Support/Chrnova/logs"
        val dirUrl = NSURL.fileURLWithPath(logDirResolved)
        NSFileManager.defaultManager.createDirectoryAtURL(
            dirUrl, withIntermediateDirectories = true, attributes = null, error = null
        )
        logFilePath = "$logDirResolved/app.log"
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun writeToFile(content: String) {
        val path = logFilePath ?: return
        val bytes = content.encodeToByteArray()
        val nsData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        nsData.writeToURL(NSURL.fileURLWithPath(path), atomically = true)
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun appendLine(line: String) {
        val existing = readAllLines()
        val newContent = (existing + line).joinToString("\n") + "\n"
        writeToFile(newContent)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun readAllLines(): List<String> {
        val path = logFilePath ?: return emptyList()
        val nsString = NSString.stringWithContentsOfFile(
            path, encoding = NSUTF8StringEncoding, error = null
        ) ?: return emptyList()
        val content = nsString as NSString
        return content.toString().lines().filter { it.isNotBlank() }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual fun rewriteLines(lines: List<String>) {
        val content = lines.joinToString("\n") + "\n"
        writeToFile(content)
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun getFileSize(): Long {
        val path = logFilePath ?: return 0L
        val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, error = null)
        return (attrs?.get("NSFileSize") as? NSNumber)?.longLongValue ?: 0L
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun clear() {
        val path = logFilePath ?: return
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}
