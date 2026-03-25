package restarhalf.stellar.schedule.core.update

data class AppUpdateInfo(
    val latestVersion: String,
    val releasePageUrl: String,
    val downloadUrl: String,
    val changelog: String,
)
