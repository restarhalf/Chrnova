package restarhalf.stellar.schedule

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.ui.sync.SyncUiState

@Stable
data class AppState(
    val campus: Campus = Campus.Jinshitan,
    val termStartMs: Long = 0L,
    val totalWeeks: Int = 0,
    val syncUiState: SyncUiState = SyncUiState.Idle,
    val isWideScreen: Boolean = false,
    val barMode: Int = 0,
    val pendingUpdate: AppUpdateInfo? = null,
    val showUpdateDialog: Boolean = false,
    val showFirstOpenDialog: Boolean = false,
    val showApkDownloadDialog: Boolean = false,
    val enablePageUserScroll: Boolean = true,
    val enableCornerClip: Boolean = true,
    val enableDim: Boolean = true,
    val blockInputDuringTransition: Boolean = true,
    val popDirectionFollowsSwipeEdge: Boolean = false,
)

val LocalAppState = compositionLocalOf<AppState> {
    error("No AppState provided!")
}

val LocalUpdateAppState = staticCompositionLocalOf<((AppState) -> AppState) -> Unit> {
    error("No AppState updater provided!")
}
