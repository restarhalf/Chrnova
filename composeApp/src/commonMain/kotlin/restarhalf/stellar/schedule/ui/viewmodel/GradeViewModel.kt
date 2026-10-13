package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.text.DecimalFormatter
import restarhalf.stellar.schedule.core.time.SemesterUtils
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.ObserveAllGradesUseCase
import kotlin.math.roundToInt


class GradeViewModel(
    observeAllGrades: ObserveAllGradesUseCase,
    private val settings: SettingsPort,
) : ViewModel() {

    
    @Immutable
    data class GradeUiState(
        val loading: Boolean,
        val error: String,
        val report: TermGradeReport,
    )

    
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

    private val _loading = MutableStateFlow(false)

    private val _error = MutableStateFlow("")

    private val _summary = MutableStateFlow(TermGradeReport())


    private var cachedGradesRef: List<GradeCourse>? = null
    private var cachedSortedSemesters: List<String>? = null

    private fun sortedSemesters(allGrades: List<GradeCourse>): List<String> {
        if (allGrades === cachedGradesRef) return cachedSortedSemesters ?: emptyList()
        val result = allGrades
            .map { it.semester }
            .distinct()
            .filter { it.isNotBlank() }
            .sortedWith(SemesterUtils.comparator)
        cachedGradesRef = allGrades
        cachedSortedSemesters = result
        return result
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _uiState: StateFlow<GradeUiState> =
        combine(
            _loading,
            _error,
            settings.observeSelectedTerm(),
            _summary,
            observeAllGrades()
        ) { loading, error, term, summary, allGrades ->
            val itemsForTerm = allGrades.filter { it.semester == term }
            val finalReport = when {

                itemsForTerm.isNotEmpty() -> {
                    summary.copy(achievements = itemsForTerm)
                }

                summary.achievements.isNotEmpty() -> {
                    summary
                }

                allGrades.isNotEmpty() -> {
                    val latestSemester = sortedSemesters(allGrades).lastOrNull()
                    val fallbackItems = allGrades.filter { it.semester == latestSemester }
                    summary.copy(achievements = fallbackItems)
                }

                else -> summary
            }
            GradeUiState(loading = loading, error = error, report = finalReport)
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

        val seenKeys = HashSet<String>()
        val cards = courses.map { grade ->
            val card = buildGradeCardUi(grade)
            if (seenKeys.add(card.idKey)) {
                card
            } else {
                card.copy(idKey = "${card.idKey}#${seenKeys.size}")
            }
        }
        val summary = GradeSummary(
            earnedCredits = report.earnedCredits,
            totalGradePoints = report.totalGradePoints,
            averageCreditGradePoint = report.averageCreditGradePoint,
        )
        val statusText =
            when {
                error.isNotBlank() -> error
                !loading && cards.isEmpty() -> "暂无成绩数据"
                else -> null
            }
        return GradeScreenUi(cards = cards.toPersistentList(), summary = summary, statusText = statusText)
    }

    
    fun buildGradeTitle(grade: GradeCourse): String = grade.courseName.ifBlank { "未命名课程" }

    
    private fun buildGradeSubtitle(grade: GradeCourse): String {
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
            appendLine("学分：${grade.credit.displayOrDash()}")
            appendLine("绩点：${DecimalFormatter.format(grade.gradePoint, 1)}")
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

    
    private fun buildGradeScoreText(grade: GradeCourse): String {
        return grade.score.ifBlank { grade.gradeLevel.ifBlank { grade.passStatus.ifBlank { "--" } } }
    }

    
    private fun buildGradeJdText(grade: GradeCourse): String {
        return "绩点：${DecimalFormatter.format(grade.gradePoint, 1)}"
    }
    
    private fun buildGradeCardUi(grade: GradeCourse): GradeCardUi {
        return GradeCardUi(
            idKey = grade.gradeId.ifBlank { grade.courseCode + grade.courseName },
            grade = grade,
            title = buildGradeTitle(grade),
            subtitle = buildGradeSubtitle(grade),
            scoreText = buildGradeScoreText(grade),
            jdText = buildGradeJdText(grade),
            isRetakeExam = grade.examinationNature.contains("补考"),
            isFailed = grade.score.toDoubleOrNull()?.let { it < 60.0 } ?: false
        )
    }

    
    fun load() {
        val loader = this.loader ?: return
        if (_loading.value) return

        _loading.value = true
        _error.value = ""

        viewModelScope.launch {
            runCatching { loader() }
                .onSuccess { _summary.value = it }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Grades", "加载成绩数据失败", e)

                    _error.value = e.toUserFacingMessage(UserFacingErrorKind.LoadGrades)
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
