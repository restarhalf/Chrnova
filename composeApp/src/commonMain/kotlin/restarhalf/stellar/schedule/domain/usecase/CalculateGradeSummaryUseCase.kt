package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import kotlin.math.roundToInt

class CalculateGradeSummaryUseCase {
    data class Summary(
        val averageScoreText: String,
        val averageGradePointText: String,
        val earnedCreditsText: String,
        val totalGradePointsText: String,
    )

    operator fun invoke(report: TermGradeReport): Summary {
        return Summary(
            averageScoreText = report.averageScoreText(),
            averageGradePointText = report.averageGradePointText(),
            earnedCreditsText = report.earnedCredits.ifBlank { "--" },
            totalGradePointsText = report.totalGradePoints.ifBlank { "--" },
        )
    }

    private fun TermGradeReport.averageScoreText(): String {
        averageScore.toDoubleOrNull()?.takeIf { !it.isNaN() && it > 0 }
            ?.let { return formatDouble(it) }

        val weighted =
            achievements.mapNotNull { grade ->
                val scoreValue = grade.score.toDoubleOrNull() ?: return@mapNotNull null
                if (grade.credit <= 0.0) return@mapNotNull null
                scoreValue to grade.credit
            }

        val creditSum = weighted.sumOf { it.second }
        if (creditSum <= 0.0) return "--"
        val total = weighted.sumOf { it.first * it.second }
        return formatDouble(total / creditSum)
    }

    private fun TermGradeReport.averageGradePointText(): String {
        averageCreditGradePoint.toDoubleOrNull()?.takeIf { !it.isNaN() && it > 0 }?.let {
            return formatDouble(it)
        }

        val total = totalGradePoints.toDoubleOrNull()
        val credits = earnedCredits.toDoubleOrNull()
        if (total == null || credits == null || credits <= 0.0) return "--"
        return formatDouble(total / credits)
    }

    private fun formatDouble(value: Double): String {
        val scaled = (value * 100).roundToInt() / 100.0
        return if (scaled % 1.0 == 0.0) scaled.toInt().toString() else DecimalFormatter.format(
            scaled,
            2
        )
    }
}
