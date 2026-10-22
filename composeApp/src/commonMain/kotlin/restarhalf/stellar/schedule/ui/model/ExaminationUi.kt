package restarhalf.stellar.schedule.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import restarhalf.stellar.schedule.domain.model.Examination

@Immutable
data class ExamCardUi(
    val idKey: String,
    val exam: Examination,
    val title: String,
    val dateText: String,
    val timeText: String,
    val locationText: String,
    val seatText: String,
    val remarkText: String?,
    val isEnded: Boolean = false,
)

@Stable
data class ExaminationScreenUi(
    val cards: ImmutableList<ExamCardUi>,
    val statusText: String?,
)
