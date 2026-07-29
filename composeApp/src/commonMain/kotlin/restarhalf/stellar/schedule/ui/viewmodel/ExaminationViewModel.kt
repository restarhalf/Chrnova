package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.SyncExamEventsToCalendarUseCase
import restarhalf.stellar.schedule.platform.AppIoDispatcher

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
    private val auth: AuthPort,
    private val settings: SettingsPort,
    private val syncExamEventsToCalendar: SyncExamEventsToCalendarUseCase,
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

    /**
     * 考试安排页面UI
     * 
     * @param cards 考试卡片列表
     * @param statusText 状态文本（如"暂无考试安排"）
     */
    @Stable
    data class ExaminationScreenUi(
        val cards: ImmutableList<ExamCardUi>,
        val statusText: String?,
    )

    /**
     * 考试安排UI状态
     * 
     * @param loading 是否正在加载
     * @param error 错误消息
     * @param items 考试列表
     */
    @Stable
    data class ExaminationUiState(
        val loading: Boolean,
        val error: String,
        val items: ImmutableList<Examination>,
    )

    private val _loading = MutableStateFlow(false)

    private val _error = MutableStateFlow("")

    private val _userNo = auth.observeProfile().map { it.userNo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "",
        )

    private val _selectedTerm = settings.observeSelectedTerm()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = "",
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _uiState: StateFlow<ExaminationUiState> =
        combine(_loading, _error, _userNo, _selectedTerm) { loading, error, userNo, term ->
            Triple(loading, error, userNo) to term
        }
            .combine(observeAllExaminations()) { pair, exams ->
                val (loading, error, userNo) = pair.first
                val term = pair.second
                val filtered = exams.filter { exam ->
                    val userMatch = if (userNo.isNotBlank()) exam.userNo == userNo else true
                    val termMatch = if (term.isNotBlank()) exam.semesterId == term else true
                    userMatch && termMatch
                }
                ExaminationUiState(loading = loading, error = error, items = filtered.toPersistentList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue =
                    ExaminationUiState(
                        loading = false,
                        error = "",
                        items = persistentListOf(),
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
        val sorted = items.sortedBy { it.time.substringBefore(" ").ifBlank { "9999-99-99" } }
        val cards = sorted.map { exam -> buildExamCardUi(exam, isExamNotEnded(exam.time, nowMs).not()) }
        val statusText =
            when {
                error.isNotBlank() -> error
                !loading && cards.isEmpty() -> "暂无考试安排"
                else -> null
            }
        return ExaminationScreenUi(cards = cards.toPersistentList(), statusText = statusText)
    }

    /** 加载考试数据 */
    fun load() {
        val loader = this.loader ?: return
        if (_loading.value) return

        _loading.value = true
        _error.value = ""

        viewModelScope.launch {
            runCatching { loader() }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("Exams", "加载考试数据失败", e)
                    _error.value = e.toUserFacingMessage(UserFacingErrorKind.LoadExaminations)
                }
            _loading.value = false
        }
    }

    /**
     * 刷新考试日历事件
     *
     * 进入考试页或考试数据变化后调用,触发日历事件全量重建。
     * 仅在已开启「考试日历提醒」时实际写入。
     */
    fun refreshExamCalendar() {
        viewModelScope.launch {
            runCatching {
                withContext(AppIoDispatcher) {
                    val term = _selectedTerm.value
                    syncExamEventsToCalendar(selectedTerm = term)
                }
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Calendar", "考试日历刷新失败", e)
            }
        }
    }

    /**
     * 构建考试卡片UI
     * 
     * @param exam 考试数据
     * @return 考试卡片UI
     */
    private fun buildExamCardUi(exam: Examination, isEnded: Boolean = false): ExamCardUi {
        val title = exam.courseName.ifBlank { "未命名课程" }
        val datePart = exam.time.substringBefore(" ").ifBlank { "待定" }
        val timePart = exam.time.substringAfter(" ", "").ifBlank { "待定" }
        val weekday = datePart.takeIf { it.isNotBlank() && it != "待定" }
            ?.let { try { LocalDate.parse(it) } catch (e: Exception) {
                AppLogger.log("Exam", "日期解析失败: $it", e)
                null
            } }
            ?.let { weekdayText(it.dayOfWeek) }
            ?: ""
        val hasWeekdayInTime = WEEKDAY_REGEX.containsMatchIn(timePart)
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
            remarkText = exam.ksbz.takeIf { it.isNotBlank() }?.let { "备注：$it" },
            isEnded = isEnded)
    }

    private fun weekdayText(dayOfWeek: DayOfWeek): String = ClockTime.weekdayShort(dayOfWeek)

    private companion object {
        val WEEKDAY_REGEX = Regex("星期[一二三四五六日]")
    }
}
