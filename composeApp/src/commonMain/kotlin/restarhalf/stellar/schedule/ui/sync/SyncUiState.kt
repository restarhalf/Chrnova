package restarhalf.stellar.schedule.ui.sync

import androidx.compose.runtime.Immutable

@Immutable
sealed interface SyncUiState {
    data object Idle : SyncUiState
    data object Loading : SyncUiState
    data class Success(val inserted: Int, val campusName: String) : SyncUiState
    data class Error(val message: String) : SyncUiState
}
