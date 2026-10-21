package restarhalf.stellar.schedule.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class ExamUi(
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val accentCourseName: String,
    val isEnded: Boolean,
    val isStarted: Boolean,
)
