package restarhalf.stellar.schedule.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import restarhalf.stellar.schedule.domain.model.GradeCourse

@Immutable
data class GradeSummary(
    val earnedCredits: String = "",
    val totalGradePoints: String = "",
    val averageCreditGradePoint: String = "",
)

@Stable
data class GradeScreenUi(
    val cards: ImmutableList<GradeCardUi>,
    val summary: GradeSummary,
    val statusText: String?,
)

@Immutable
data class GradeCardUi(
    val idKey: String,
    val grade: GradeCourse,
    val title: String,
    val subtitle: String,
    val scoreText: String,
    val jdText: String,
    val isRetakeExam: Boolean,
    val isFailed: Boolean = false,
)
