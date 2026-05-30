package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import restarhalf.stellar.schedule.domain.usecase.ObserveAllGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveSelectedTermUseCase
import kotlin.math.roundToInt

class GradeViewModel(
    observeAllGrades: ObserveAllGradesUseCase,
    observeSelectedTerm: ObserveSelectedTermUseCase,
) : ViewModel() {

    data class GradeUiState(
        val loading: Boolean,
        val error: String,
        val report: TermGradeReport,
    )

    data class GradeScreenUi(
        val cards: List<GradeCardUi>,
        val statusText: String?,
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

    private val _summary = MutableStateFlow(TermGradeReport())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _uiState: StateFlow<GradeUiState> =
        combine(
            _loading,
            _error,
            observeSelectedTerm(),
            _summary,
            observeAllGrades()
        ) { loading, error, term, summary, allGrades ->
            val itemsForTerm = allGrades.filter { it.semester == term }
            val finalReport = when {
                // 1. 优先显示当前选择学期的本地数据
                itemsForTerm.isNotEmpty() -> {
                    summary.copy(achievements = itemsForTerm)
                }
                // 2. 如果当前学期本地没数据，但 summary 有数据（说明刚从网络/Fallback 拿到）
                summary.achievements.isNotEmpty() -> {
                    summary
                }
                // 3. 断网状态且本地没当前学期数据，尝试寻找本地最新的其他学期数据作为回退
                allGrades.isNotEmpty() -> {
                    val latestSemester = allGrades
                        .map { it.semester }
                        .distinct()
                        .filter { it.isNotBlank() }
                        .sortedWith(SemesterComparator)
                        .lastOrNull()
                    
                    val fallbackItems = allGrades.filter { it.semester == latestSemester }
                    summary.copy(achievements = fallbackItems)
                }
                // 4. 彻底没数据
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

    private object SemesterComparator : Comparator<String> {
        override fun compare(a: String, b: String): Int {
            val ka = parse(a)
            val kb = parse(b)
            return when {
                ka != null && kb != null -> {
                    if (ka.first != kb.first) ka.first.compareTo(kb.first)
                    else if (ka.second != kb.second) ka.second.compareTo(kb.second)
                    else ka.third.compareTo(kb.third)
                }
                ka != null -> 1
                kb != null -> -1
                else -> a.compareTo(b)
            }
        }

        private fun parse(id: String): Triple<Int, Int, Int>? {
            val parts = id.trim().split("-")
            if (parts.size < 3) return null
            val y1 = parts[0].toIntOrNull() ?: return null
            val y2 = parts[1].toIntOrNull() ?: return null
            val t = parts[2].toIntOrNull() ?: return null
            return Triple(y1, y2, t)
        }
    }

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
        return GradeScreenUi(cards = cards, statusText = statusText)
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
                .onSuccess { _summary.value = it }
                .onFailure {
                    _summary.value = TermGradeReport()
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
