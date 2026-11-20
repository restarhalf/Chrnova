@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class,
)

package restarhalf.stellar.schedule.data.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.posix.memcpy

class NsFileAppFile(private val filePath: String) : AppFile {

    override val path: String get() = filePath

    override fun exists(): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(filePath)

    override fun readBytes(): ByteArray =
        NSFileManager.defaultManager.contentsAtPath(filePath)?.toByteArray()
            ?: error("文件不存在或读取失败: $filePath")

    override fun writeBytes(bytes: ByteArray) {
        parentOf(filePath)?.let { createDirectory(it) }
        if (!bytes.toNSData().writeToFile(filePath, atomically = true)) {
            error("文件写入失败: $filePath")
        }
    }

    override fun mkdirs() {
        createDirectory(filePath)
    }

    override fun delete(): Boolean =
        NSFileManager.defaultManager.removeItemAtPath(filePath, null)

    override fun resolve(relative: String): AppFile = NsFileAppFile("$filePath/$relative")

    override fun toString(): String = "NsFileAppFile($filePath)"
}

private fun createDirectory(path: String) {
    NSFileManager.defaultManager.createDirectoryAtPath(
        path,
        withIntermediateDirectories = true,
        attributes = null,
        error = null,
    )
}

private fun parentOf(path: String): String? {
    val trimmed = path.trimEnd('/')
    val idx = trimmed.lastIndexOf('/')
    return if (idx > 0) trimmed.substring(0, idx) else null
}

fun defaultDocumentsDir(): String =
    (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String)
        ?: error("无法定位 Documents 目录")

private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData.create(bytes = null, length = 0u)
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
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
