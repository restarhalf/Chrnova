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

/**
 * 成绩查询ViewModel
 * 
 * 管理成绩查询页面的UI状态，包括：
 * - 成绩数据加载和显示
 * - 学期筛选
 * - 成绩卡片UI构建
 * - 成绩详情展示
 */
class GradeViewModel(
    observeAllGrades: ObserveAllGradesUseCase,
    private val settings: SettingsPort,
) : ViewModel() {

    /**
     * 成绩UI状态
     * 
     * @param loading 是否正在加载
     * @param error 错误消息
     * @param report 学期成绩报告
     */
    @Immutable
    data class GradeUiState(
        val loading: Boolean,
        val error: String,
        val report: TermGradeReport,
    )

    /**
     * 成绩汇总信息
     *
     * @param earnedCredits 已获得学分
     * @param totalGradePoints 总绩点
     * @param averageCreditGradePoint 平均学分绩点
     */
    @Immutable
    data class GradeSummary(
        val earnedCredits: String = "",
        val totalGradePoints: String = "",
        val averageCreditGradePoint: String = "",
    )

    /**
     * 成绩页面UI
     * 
     * @param cards 成绩卡片列表
     * @param summary 成绩汇总信息
     * @param statusText 状态文本（如"暂无成绩数据"）
     */
    @Stable
    data class GradeScreenUi(
        val cards: ImmutableList<GradeCardUi>,
        val summary: GradeSummary,
        val statusText: String?,
    )

    /**
     * 成绩卡片UI数据
     *
     * @param idKey 唯一标识
     * @param grade 课程成绩数据
     * @param title 课程名称
     * @param subtitle 副标题（课程代码、学分等）
     * @param scoreText 成绩文本
     * @param jdText 课程绩点
     * @param isRetakeExam 是否为补考
     * @param isFailed 是否挂科（成绩<60）
     */
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

    // 缓存排序后的学期列表，避免每次 combine 都重新排序
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
                // 优先显示当前选择学期的本地数据
                itemsForTerm.isNotEmpty() -> {
                    summary.copy(achievements = itemsForTerm)
                }
                // 如果当前学期本地没数据，但 summary 有数据（说明刚从网络/Fallback 拿到）
                summary.achievements.isNotEmpty() -> {
                    summary
                }
                // 断网状态且本地没当前学期数据，尝试寻找本地最新的其他学期数据作为回退
                allGrades.isNotEmpty() -> {
                    val latestSemester = sortedSemesters(allGrades).lastOrNull()
                    val fallbackItems = allGrades.filter { it.semester == latestSemester }
                    summary.copy(achievements = fallbackItems)
                }
                // 彻底没数据
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

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<GradeUiState> = _uiState

    private var loader: (suspend () -> TermGradeReport)? = null

    /**
     * 绑定数据加载器
     * 
     * @param loader 加载成绩数据的挂起函数
     */
    fun bindLoader(loader: suspend () -> TermGradeReport) {
        this.loader = loader
    }

    /**
     * 构建成绩页面UI
     * 
     * @param report 学期成绩报告
     * @param loading 是否正在加载
     * @param error 错误消息
     * @return 成绩页面UI
     */
    fun buildScreenUi(
        report: TermGradeReport,
        loading: Boolean,
        error: String,
    ): GradeScreenUi {
        val courses = report.achievements
        val cards = courses.map { buildGradeCardUi(it) }
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

    /**
     * 构建成绩标题
     * 
     * @param grade 成绩数据
     * @return 课程名称
     */
    fun buildGradeTitle(grade: GradeCourse): String = grade.courseName.ifBlank { "未命名课程" }

    /**
     * 构建成绩副标题
     * 
     * @param grade 成绩数据
     * @return 包含课程代码和学分的副标题
     */
    private fun buildGradeSubtitle(grade: GradeCourse): String {
        return listOf(
            grade.courseCode.takeIf { it.isNotBlank() },
            "学分:${grade.credit.displayOrDash()}",
        )
            .joinToString(" · ")
            .ifBlank { grade.passStatus.ifBlank { "暂无补充信息" } }
    }

    /**
     * 构建成绩详情摘要
     * 
     * @param grade 成绩数据
     * @return 格式化的成绩详情文本
     */
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

    /**
     * 构建成绩分数文本
     * 
     * @param grade 成绩数据
     * @return 分数、等级或通过状态
     */
    private fun buildGradeScoreText(grade: GradeCourse): String {
        return grade.score.ifBlank { grade.gradeLevel.ifBlank { grade.passStatus.ifBlank { "--" } } }
    }

    /**
     * 构建绩点文本
     *
     * @param grade 成绩数据
     * @retrun 绩点信息
     */
    private fun buildGradeJdText(grade: GradeCourse): String {
        return "绩点：${DecimalFormatter.format(grade.gradePoint, 1)}"
    }
    /**
     * 构建成绩卡片UI
     * 
     * @param grade 成绩数据
     * @return 成绩卡片UI
     */
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

    /** 加载成绩数据 */
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
                    _summary.value = TermGradeReport()
                    _error.value = e.toUserFacingMessage(UserFacingErrorKind.LoadGrades)
                }
            _loading.value = false
        }
    }

    /**
     * Double扩展函数，用于格式化显示
     * 
     * @return 格式化后的字符串
     */
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
