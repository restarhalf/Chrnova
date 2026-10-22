package restarhalf.stellar.schedule.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class AboutScreenUi(
    val versionDisplay: String,
    val currentVersionForCheck: String,
    val updateActionSummary: String,
    val canCheckUpdate: Boolean,
)
