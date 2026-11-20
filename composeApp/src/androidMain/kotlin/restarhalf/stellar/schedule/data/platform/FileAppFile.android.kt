package restarhalf.stellar.schedule.data.platform

import java.io.File

class FileAppFile(private val file: File) : AppFile {

    override val path: String get() = file.path

    override fun exists(): Boolean = file.exists()

    override fun readBytes(): ByteArray = file.readBytes()

    override fun writeBytes(bytes: ByteArray) {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    override fun mkdirs() {
        file.mkdirs()
    }

    override fun delete(): Boolean = file.delete()

    override fun resolve(relative: String): AppFile = FileAppFile(File(file, relative))

    override fun toString(): String = "FileAppFile($path)"

    companion object {
        fun from(path: String): FileAppFile = FileAppFile(File(path))
    }
}
