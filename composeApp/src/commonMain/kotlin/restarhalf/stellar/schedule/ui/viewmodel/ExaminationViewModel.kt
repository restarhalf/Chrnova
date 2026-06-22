package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAuthProfileUseCase

/**
 * 考试安排ViewModel
 * 
 * 管理考试安排页面的UI状态，包括：
 * - 考试列表加载和显示
 * - 过滤已结束的考试
 * - 构建考试卡片UI
 */
class ExaminationViewModel(
    private val isExamNotEnded: IsExamNotEndedUseCase,
    observeAllExaminations: ObserveAllExaminationsUseCase,
    observeAuthProfile: ObserveAuthProfileUseCase,
) : ViewModel() {

    /**
     * 考试卡片UI
     * 
     * @param idKey 唯一标识符
     * @param exam 考试数据
     * @param title 课程名称
     * @param dateText 考试日期文本
     * @param timeText 考试时间文本
     * @param locationText 考试地点文本
     * @param seatText 座位号文本
     * @param remarkText 备注文本
     */
    data class ExamCardUi(
        val idKey: String,
        val exam: Examination,
        val title: String,
        val dateText: String,
        val timeText: String,
        val locationText: String,
        val seatText: String,
        val remarkText: String?,
    )

    /**
     * 考试安排页面UI
     * 
     * @param cards 考试卡片列表
     * @param statusText 状态文本（如"暂无考试安排"）
     */
    data class ExaminationScreenUi(
        val cards: List<ExamCardUi>,
        val statusText: String?,
    )

    /**
     * 考试安排UI状态
     * 
     * @param loading 是否正在加载
     * @param error 错误消息
     * @param items 考试列表
     */
    data class ExaminationUiState(
        val loading: Boolean,
        val error: String,
        val items: List<Examination>,
    )

    private val _loading = MutableStateFlow(false)

    private val _error = MutableStateFlow("")

    private val _userNo = observeAuthProfile().map { it.userNo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "",
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _uiState: StateFlow<ExaminationUiState> =
        combine(_loading, _error, _userNo) { loading, error, userNo ->
            Triple(loading, error, userNo)
        }
            .combine(observeAllExaminations()) { triple, exams ->
                val (loading, error, userNo) = triple
                val filtered = if (userNo.isNotBlank()) {
                    exams.filter { it.userNo == userNo }
                } else {
                    exams
                }
                ExaminationUiState(loading = loading, error = error, items = filtered)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    ExaminationUiState(
                        loading = false,
                        error = "",
                        items = emptyList(),
                    ),
            )

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<ExaminationUiState> = _uiState

    private var loader: (suspend () -> List<Examination>)? = null

    /**
     * 绑定数据加载器
     * 
     * @param loader 加载考试数据的挂起函数
     */
    fun bindLoader(loader: suspend () -> List<Examination>) {
        this.loader = loader
    }

    /**
     * 构建考试安排页面UI
     * 
     * @param items 考试列表
     * @param loading 是否正在加载
     * @param error 错误消息
     * @param nowMs 当前时间戳
     * @return 考试安排页面UI
     */
    fun buildScreenUi(
        items: List<Examination>,
        loading: Boolean,
        error: String,
        nowMs: Long,
    ): ExaminationScreenUi {
        // 过滤已结束的考试
        val visibleItems = items.filter { isExamNotEnded(it.time, nowMs) }
        val cards = visibleItems.map { exam -> buildExamCardUi(exam) }
        val statusText =
            when {
                error.isNotBlank() -> error
                !loading && cards.isEmpty() -> "暂无考试安排"
                else -> null
            }
        return ExaminationScreenUi(cards = cards, statusText = statusText)
    }

    /** 加载考试数据 */
    fun load() {
        val loader = this.loader ?: return
        if (_loading.value) return

        _loading.value = true
        _error.value = ""

        viewModelScope.launch {
            runCatching { loader() }
                .onFailure {
                    AppLogger.log("Exams", "加载考试数据失败", it)
                    _error.value = it.toUserFacingMessage(UserFacingErrorKind.LoadExaminations)
                }
            _loading.value = false
        }
    }

    /**
     * 构建考试卡片UI
     * 
     * @param exam 考试数据
     * @return 考试卡片UI
     */
    private fun buildExamCardUi(exam: Examination): ExamCardUi {
        val title = exam.courseName.ifBlank { "未命名课程" }
        val datePart = exam.time.substringBefore(" ").ifBlank { "待定" }
        val timePart = exam.time.substringAfter(" ", "").ifBlank { "待定" }
        val weekday = datePart.takeIf { it.isNotBlank() && it != "待定" }
            ?.let { try { LocalDate.parse(it) } catch (_: Exception) { null } }
            ?.let { weekdayText(it.dayOfWeek) }
            ?: ""
        val hasWeekdayInTime = timePart.contains(Regex("星期[一二三四五六日]"))
        val dateText = "考试日期:$datePart"
        val timeText = if (weekday.isNotBlank() && !hasWeekdayInTime) {
            "考试时间:星期$weekday $timePart"
        } else {
            "考试时间:$timePart"
        }
        return ExamCardUi(
            idKey = exam.courseNumber + exam.zwh + exam.time,
            exam = exam,
            title = title,
            dateText = dateText,
            timeText = timeText,
            locationText = "地点：${exam.examinationPlace.ifBlank { "待定" }}",
            seatText = "座位号：${exam.zwh.ifBlank { "--" }}",
            remarkText = exam.ksbz.takeIf { it.isNotBlank() }?.let { "备注：$it" })
    }

    private fun weekdayText(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "一"
            DayOfWeek.TUESDAY -> "二"
            DayOfWeek.WEDNESDAY -> "三"
            DayOfWeek.THURSDAY -> "四"
            DayOfWeek.FRIDAY -> "五"
            DayOfWeek.SATURDAY -> "六"
            DayOfWeek.SUNDAY -> "日"
        }
    }
}
