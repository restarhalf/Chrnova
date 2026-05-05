package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.usecase.CalculateGradeSummaryUseCase
import kotlin.math.roundToInt

class GradeViewModel(
    private val calculateGradeSummary: CalculateGradeSummaryUseCase,
) : ViewModel() {

    data class GradeUiState(
        val loading: Boolean,
        val error: String,
        val report: TermGradeReport,
    )

    data class GradeSummaryUi(
        val averageScoreText: String,
        val averageGradePointText: String,
        val earnedCreditsText: String,
        val totalGradePointsText: String,
    )

    data class GradeScreenUi(
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

    private val _error = MutableStateFlow("")

    private val _report = MutableStateFlow(TermGradeReport())

    private val _uiState: StateFlow<GradeUiState> =
        combine(_loading, _error, _report) { loading, error, report ->
            GradeUiState(loading = loading, error = error, report = report)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    GradeUiState(
                        loading = false,
                        error = "",
                        report = TermGradeReport(),
                    ),
            )

    val uiState: StateFlow<GradeUiState> = _uiState

    private var loader: (suspend () -> TermGradeReport)? = null

    fun bindLoader(loader: suspend () -> TermGradeReport) {
        this.loader = loader
    }

    fun buildScreenUi(
        report: TermGradeReport,
        loading: Boolean,
        error: String,
    ): GradeScreenUi {
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
                val calculatedSummary = calculateGradeSummary(report)
                GradeSummaryUi(
                    averageScoreText = calculatedSummary.averageScoreText,
                    averageGradePointText = calculatedSummary.averageGradePointText,
                    earnedCreditsText = calculatedSummary.earnedCreditsText,
                    totalGradePointsText = calculatedSummary.totalGradePointsText,
                )
            } else {
                null
            }
        return GradeScreenUi(cards = cards, statusText = statusText, summary = summary)
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

    private fun formatDouble(value: Double): String {
        val scaled = (value * 100).roundToInt() / 100.0
        return if (scaled % 1.0 == 0.0) scaled.toInt().toString() else DecimalFormatter.format(
            scaled,
            2
        )
    }

}
