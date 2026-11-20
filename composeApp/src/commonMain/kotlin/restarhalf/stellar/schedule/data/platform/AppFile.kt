package restarhalf.stellar.schedule.data.platform

interface AppFile {
    val path: String

    fun exists(): Boolean

    fun readBytes(): ByteArray

    fun readText(): String = readBytes().decodeToString()

    fun writeBytes(bytes: ByteArray)

    fun mkdirs()

    fun delete(): Boolean = false

    fun resolve(relative: String): AppFile
}

object AppFiles {
    private var rootFile: AppFile? = null

    val isReady: Boolean get() = rootFile != null

    fun init(root: AppFile) {
        rootFile = root
    }

    val root: AppFile
        get() = rootFile ?: error(
            "AppFiles 未初始化：请在 Application.onCreate() / AppRoot() 中调用 AppFiles.init(root)"
        )
}
