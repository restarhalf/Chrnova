package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import kotlin.math.roundToInt

class GradeViewModel : ViewModel() {

    data class GradeSummaryUi(
        val averageScoreText: String,
        val averageGradePointText: String,
        val earnedCreditsText: String,
        val totalGradePointsText: String,
    )

    data class ScreenState(
        val cards: List<GradeCardUi>,
        val statusText: String?,
        val summary: GradeSummaryUi?,
    )

    data class GradeCardUi(
        val idKey: String,
        val grade: GradeCourse,
        val title: String,
        val subtitle: String,
        val scoreText: String,
        val attrText: String,
        val isRetakeExam: Boolean,
    )

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error.asStateFlow()

    private val _report = MutableStateFlow(TermGradeReport())
    val report: StateFlow<TermGradeReport> = _report.asStateFlow()

    private var loader: (suspend () -> TermGradeReport)? = null

    fun bindLoader(loader: suspend () -> TermGradeReport) {
        this.loader = loader
    }

    fun buildScreenState(
        report: TermGradeReport,
        loading: Boolean,
        error: String,
    ): ScreenState {
        val courses = report.achievements
        val cards = courses.map { buildGradeCardUi(it) }
        val statusText =
            when {
                error.isNotBlank() -> error
                !loading && cards.isEmpty() -> "暂无成绩数据"
                else -> null
            }
        val summary =
            if (courses.isNotEmpty()) {
                GradeSummaryUi(
                    averageScoreText = report.averageScoreText(),
                    averageGradePointText = report.averageGradePointText(),
                    earnedCreditsText = report.earnedCredits.ifBlank { "--" },
                    totalGradePointsText = report.totalGradePoints.ifBlank { "--" },
                )
            } else {
                null
            }
        return ScreenState(cards = cards, statusText = statusText, summary = summary)
    }

    fun buildGradeTitle(grade: GradeCourse): String = grade.courseName.ifBlank { "未命名课程" }

    fun buildGradeSubtitle(grade: GradeCourse): String {
        return listOf(
            grade.courseCode.takeIf { it.isNotBlank() },
            "学分:${grade.credit.displayOrDash()}",
        )
            .joinToString(" · ")
            .ifBlank { grade.passStatus.ifBlank { "暂无补充信息" } }
    }

    fun buildGradeDetailsSummary(grade: GradeCourse): String {
        return buildString {
            appendLine("课程号：${grade.courseCode.ifBlank { "暂无" }}")
            appendLine("成绩：${buildGradeScoreText(grade)}")
            appendLine("绩点：${grade.gradePoint.displayOrDash()}")
            appendLine("学分：${grade.credit.displayOrDash()}")
            appendLine("课程属性：${grade.curriculumAttributes.ifBlank { "暂无" }}")
            appendLine("课程性质：${grade.courseNature.ifBlank { "暂无" }}")
            appendLine("考核方式：${grade.examName.ifBlank { "暂无" }}")
            appendLine("考试性质：${grade.examinationNature.ifBlank { "暂无" }}")
            appendLine("是否及格：${grade.passStatus.ifBlank { "暂无" }}")
            if (grade.gradeLevel.isNotBlank()) appendLine("等级：${grade.gradeLevel}")
            if (grade.repeatSemester.isNotBlank()) appendLine("补重修学期：${grade.repeatSemester}")
            if (grade.markFlag.isNotBlank()) appendLine("成绩标识：${grade.markFlag}")
            if (grade.semester.isNotBlank()) appendLine("开课学期：${grade.semester}")
        }
            .trim()
    }

    fun buildGradeScoreText(grade: GradeCourse): String {
        return grade.score.ifBlank { grade.gradeLevel.ifBlank { grade.passStatus.ifBlank { "--" } } }
    }

    private fun buildGradeCardUi(grade: GradeCourse): GradeCardUi {
        return GradeCardUi(
            idKey = grade.gradeId.ifBlank { grade.courseCode + grade.courseName },
            grade = grade,
            title = buildGradeTitle(grade),
            subtitle = buildGradeSubtitle(grade),
            scoreText = buildGradeScoreText(grade),
            attrText = grade.curriculumAttributes,
            isRetakeExam = grade.examinationNature.contains("补考")
        )
    }

    fun load() {
        val loader = this.loader ?: return
        if (_loading.value) return

        _loading.value = true
        _error.value = ""

        viewModelScope.launch {
            runCatching { loader() }
                .onSuccess { _report.value = it }
                .onFailure {
                    _report.value = TermGradeReport()
                    _error.value = it.toUserFacingMessage(UserFacingErrorKind.LoadGrades)
                }
            _loading.value = false
        }
    }

    private fun Double.displayOrDash(): String {
        return if (this <= 0.0) "0" else formatDouble(this)
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
