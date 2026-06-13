package restarhalf.stellar.schedule.core.update

/**
 * APK下载状态密封接口
 * 
 * 表示Android应用更新下载的不同状态。
 */
sealed interface ApkDownloadState {
    /** 空闲状态，未开始下载 */
    data object Idle : ApkDownloadState

    /** 下载中状态 */
    data class Downloading(
        /** 下载进度（0.0-1.0） */
        val progress: Float,
        /** 已下载字节数 */
        val downloadedBytes: Long,
        /** 总字节数 */
        val totalBytes: Long,
        /** 下载文件保存路径 */
        val filePath: String,
    ) : ApkDownloadState

    /** 下载完成状态 */
    data class Completed(
        /** 下载文件保存路径 */
        val filePath: String
    ) : ApkDownloadState

    /** 下载失败状态 */
    data class Error(
        /** 错误消息 */
        val message: String
    ) : ApkDownloadState
}
