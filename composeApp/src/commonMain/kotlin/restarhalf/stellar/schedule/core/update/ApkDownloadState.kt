package restarhalf.stellar.schedule.core.update

sealed interface ApkDownloadState {
    data object Idle : ApkDownloadState

    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val filePath: String,
    ) : ApkDownloadState

    data class Completed(val filePath: String) : ApkDownloadState

    data class Error(val message: String) : ApkDownloadState
}
