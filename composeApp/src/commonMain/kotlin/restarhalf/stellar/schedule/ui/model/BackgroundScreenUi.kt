package restarhalf.stellar.schedule.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class BackgroundScreenUi(
    val hasCustomImage: Boolean,
    val imageSummary: String,
    val backgroundAlphaPercent: String,
    val backgroundBlurPercent: String,
    val componentsAlphaPercent: String,
)
