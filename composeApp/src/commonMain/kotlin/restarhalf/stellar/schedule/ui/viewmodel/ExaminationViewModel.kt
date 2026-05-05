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
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase

class ExaminationViewModel(
    private val isExamNotEnded: IsExamNotEndedUseCase,
) : ViewModel() {

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

    data class ScreenState(
        val cards: List<ExamCardUi>,
        val statusText: String?,
    )

    data class ExaminationUiState(
        val loading: Boolean,
        val error: String,
        val items: List<Examination>,
    )

    private val _loading = MutableStateFlow(false)

    private val _error = MutableStateFlow("")

    private val _items = MutableStateFlow<List<Examination>>(emptyList())

    private val _uiState: StateFlow<ExaminationUiState> =
        combine(_loading, _error, _items) { loading, error, items ->
            ExaminationUiState(loading = loading, error = error, items = items)
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

    val uiState: StateFlow<ExaminationUiState> = _uiState

    private var loader: (suspend () -> List<Examination>)? = null

    fun bindLoader(loader: suspend () -> List<Examination>) {
        this.loader = loader
    }

    fun buildScreenState(
        items: List<Examination>,
        loading: Boolean,
        error: String,
        nowMs: Long,
    ): ScreenState {
        val visibleItems = items.filter { isExamNotEnded(it.time, nowMs) }
        val cards = visibleItems.map { exam -> buildExamCardUi(exam) }
        val statusText =
            when {
                error.isNotBlank() -> error
                !loading && cards.isEmpty() -> "暂无考试安排"
                else -> null
            }
        return ScreenState(cards = cards, statusText = statusText)
    }

    fun load() {
        val loader = this.loader ?: return
        if (_loading.value) return

        _loading.value = true
        _error.value = ""

        viewModelScope.launch {
            runCatching { loader() }
                .onSuccess { _items.value = it }
                .onFailure {
                    _error.value = it.toUserFacingMessage(UserFacingErrorKind.LoadExaminations)
                }
            _loading.value = false
        }
    }

    private fun buildExamCardUi(exam: Examination): ExamCardUi {
        val title = exam.courseName.ifBlank { "未命名课程" }
        val datePart = exam.time.substringBefore(" ").ifBlank { "待定" }
        val timePart = exam.time.substringAfter(" ", "").ifBlank { "待定" }
        return ExamCardUi(
            idKey = exam.courseNumber + exam.zwh + exam.time,
            exam = exam,
            title = title,
            dateText = "考试日期:$datePart",
            timeText = "考试时间:$timePart",
            locationText = "地点：${exam.examinationPlace.ifBlank { "待定" }}",
            seatText = "座位号：${exam.zwh.ifBlank { "--" }}",
            remarkText = exam.ksbz.takeIf { it.isNotBlank() }?.let { "备注：$it" })
    }
}
