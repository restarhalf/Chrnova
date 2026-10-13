package restarhalf.stellar.schedule.core.update

import androidx.compose.runtime.Immutable


@Immutable
data class AppUpdateInfo(
    
    val latestVersion: String,
    
    val releasePageUrl: String,
    
    val downloadUrl: String,
    
    val changelog: String,
)
