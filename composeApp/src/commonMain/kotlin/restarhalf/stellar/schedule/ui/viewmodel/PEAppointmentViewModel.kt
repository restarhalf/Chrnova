package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.PEAppointmentDetail
import restarhalf.stellar.schedule.data.remote.PEAppointmentItem
import restarhalf.stellar.schedule.data.remote.PEAppointmentTimeSlot
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.usecase.PEAppointmentDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PEAppointmentListUseCase
import restarhalf.stellar.schedule.domain.usecase.PECancelAppointmentUseCase
import restarhalf.stellar.schedule.domain.usecase.PEEnterAppointmentUseCase

/**
 * 学生体测预约 ViewModel
 *
 * 对齐真实接口：
 * - 列表：selectUserAppointmentList（type "1"/"2"）
 * - 预约：详情 → 选日期 → 选时段 → appointmentEnter(appointment_id, times_id, enter_date)
 * - 取消：cancelRegistration(temporary_id)
 */
class PEAppointmentViewModel(
    private val appointmentListUseCase: PEAppointmentListUseCase,
    private val appointmentDetailUseCase: PEAppointmentDetailUseCase,
    private val cancelAppointmentUseCase: PECancelAppointmentUseCase,
    private val enterAppointmentUseCase: PEEnterAppointmentUseCase,
    private val peAuth: PEAuthPort,
) : ViewModel() {

    @Stable
    data class BookingSheetState(
        val item: PEAppointmentItem,
        val detail: PEAppointmentDetail? = null,
        val selectedDate: String? = null,
        val timeSlots: ImmutableList<PEAppointmentTimeSlot> = persistentListOf(),
        val selectedTimesId: String? = null,
        val loadingDetail: Boolean = false,
        val loadingTimes: Boolean = false,
        val loadedTimes: Boolean = false,
        val error: String? = null,
    )

    @Stable
    data class AppointmentUiState(
        val selectedTab: Int = 0,
        val myItems: ImmutableList<PEAppointmentItem> = persistentListOf(),
        val availableItems: ImmutableList<PEAppointmentItem> = persistentListOf(),
        val myTotalRows: Int = 0,
        val availableTotalRows: Int = 0,
        val myPageNum: Int = 1,
        val availablePageNum: Int = 1,
        val myHasMore: Boolean = true,
        val availableHasMore: Boolean = true,
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val loadedMy: Boolean = false,
        val loadedAvailable: Boolean = false,
        val error: String? = null,
        val actionMessage: String? = null,
        val cancelTarget: PEAppointmentItem? = null,
        val booking: BookingSheetState? = null,
        val actionInFlight: Boolean = false,
    )

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState

    val isLoggedIn: StateFlow<Boolean> = peAuth.observeToken()
        .map { token -> token.isNotBlank() }
        .catch { e ->
            if (e is CancellationException) throw e
            AppLogger.log("PEAppoint", "观察token变化异常", e)
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        when (index) {
            0 -> if (!_uiState.value.loadedMy) loadMyAppointments()
            1 -> if (!_uiState.value.loadedAvailable) loadAvailableAppointments()
        }
    }

    fun refreshCurrent() {
        when (_uiState.value.selectedTab) {
            0 -> loadMyAppointments()
            else -> loadAvailableAppointments()
        }
    }

    fun loadMyAppointments() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, error = null, myPageNum = 1, myHasMore = true)
            }
            try {
                val response = appointmentListUseCase(type = TYPE_MY, pageNum = 1)
                val items = response.data?.dataList.orEmpty().toPersistentList()
                val total = response.data?.totalRowsInt ?: items.size
                _uiState.update {
                    it.copy(
                        myItems = items,
                        myTotalRows = total,
                        myPageNum = 2,
                        myHasMore = items.size < total && items.isNotEmpty(),
                        loadedMy = true,
                        loading = false,
                        error = null,
                    )
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEAppoint", "加载我的预约失败", ex)
                _uiState.update {
                    it.copy(
                        loading = false,
                        loadedMy = true,
                        error = ex.toUserFacingMessage(UserFacingErrorKind.LoadPEAppointments),
                    )
                }
            }
        }
    }

    fun loadAvailableAppointments() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true, error = null, availablePageNum = 1, availableHasMore = true)
            }
            try {
                val response = appointmentListUseCase(type = TYPE_AVAILABLE, pageNum = 1)
                val items = response.data?.dataList.orEmpty().toPersistentList()
                val total = response.data?.totalRowsInt ?: items.size
                _uiState.update {
                    it.copy(
                        availableItems = items,
                        availableTotalRows = total,
                        availablePageNum = 2,
                        availableHasMore = items.size < total && items.isNotEmpty(),
                        loadedAvailable = true,
                        loading = false,
                        error = null,
                    )
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEAppoint", "加载可预约列表失败", ex)
                _uiState.update {
                    it.copy(
                        loading = false,
                        loadedAvailable = true,
                        error = ex.toUserFacingMessage(UserFacingErrorKind.LoadPEAppointments),
                    )
                }
            }
        }
    }

    fun loadMoreMy() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || !state.myHasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val response = appointmentListUseCase(type = TYPE_MY, pageNum = state.myPageNum)
                val page = response.data?.dataList.orEmpty()
                val total = response.data?.totalRowsInt ?: (state.myItems.size + page.size)
                _uiState.update {
                    val merged = (it.myItems + page).toPersistentList()
                    it.copy(
                        myItems = merged,
                        myTotalRows = total,
                        myPageNum = it.myPageNum + 1,
                        myHasMore = page.isNotEmpty() && merged.size < total,
                        loadingMore = false,
                    )
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEAppoint", "加载更多我的预约失败", ex)
                _uiState.update { it.copy(loadingMore = false) }
            }
        }
    }

    fun loadMoreAvailable() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || !state.availableHasMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            try {
                val response = appointmentListUseCase(type = TYPE_AVAILABLE, pageNum = state.availablePageNum)
                val page = response.data?.dataList.orEmpty()
                val total = response.data?.totalRowsInt ?: (state.availableItems.size + page.size)
                _uiState.update {
                    val merged = (it.availableItems + page).toPersistentList()
                    it.copy(
                        availableItems = merged,
                        availableTotalRows = total,
                        availablePageNum = it.availablePageNum + 1,
                        availableHasMore = page.isNotEmpty() && merged.size < total,
                        loadingMore = false,
                    )
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEAppoint", "加载更多可预约失败", ex)
                _uiState.update { it.copy(loadingMore = false) }
            }
        }
    }

    fun requestCancel(item: PEAppointmentItem) {
        if (item.temporaryId.isBlank()) {
            _uiState.update { it.copy(actionMessage = "缺少预约标识，无法取消") }
            return
        }
        _uiState.update { it.copy(cancelTarget = item) }
    }

    fun dismissCancel() {
        _uiState.update { it.copy(cancelTarget = null) }
    }

    /** 打开预约面板：拉详情，并自动选中第一个可约日期 */
    fun openBooking(item: PEAppointmentItem) {
        if (item.appointmentId.isBlank()) {
            _uiState.update { it.copy(actionMessage = "缺少场次标识，无法预约") }
            return
        }
        _uiState.update {
            it.copy(
                booking = BookingSheetState(
                    item = item,
                    loadingDetail = true,
                ),
            )
        }
        viewModelScope.launch {
            try {
                val response = appointmentDetailUseCase.detail(
                    appointmentId = item.appointmentId,
                    appointmentStatus = item.appointmentStatus,
                )
                val detail = response.data
                val firstDate = detail?.availableDates?.firstOrNull()
                _uiState.update { s ->
                    val booking = s.booking ?: return@update s
                    s.copy(
                        booking = booking.copy(
                            detail = detail,
                            selectedDate = firstDate,
                            loadingDetail = false,
                            error = null,
                        ),
                    )
                }
                if (firstDate != null) {
                    loadTimes(firstDate)
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEAppoint", "加载预约详情失败", ex)
                _uiState.update { s ->
                    val booking = s.booking ?: return@update s
                    s.copy(
                        booking = booking.copy(
                            loadingDetail = false,
                            error = ex.toUserFacingMessage(UserFacingErrorKind.LoadPEAppointments),
                        ),
                    )
                }
            }
        }
    }

    fun dismissBooking() {
        _uiState.update { it.copy(booking = null) }
    }

    fun selectBookingDate(date: String) {
        _uiState.update { s ->
            val booking = s.booking ?: return@update s
            s.copy(
                booking = booking.copy(
                    selectedDate = date,
                    selectedTimesId = null,
                    timeSlots = persistentListOf(),
                    loadedTimes = false,
                    error = null,
                ),
            )
        }
        loadTimes(date)
    }

    fun selectTimeSlot(timesId: String) {
        _uiState.update { s ->
            val booking = s.booking ?: return@update s
            s.copy(booking = booking.copy(selectedTimesId = timesId))
        }
    }

    private fun loadTimes(date: String) {
        val bookingId = _uiState.value.booking?.item?.appointmentId ?: return
        _uiState.update { s ->
            val booking = s.booking ?: return@update s
            s.copy(booking = booking.copy(loadingTimes = true, error = null))
        }
        viewModelScope.launch {
            try {
                val response = appointmentDetailUseCase.times(
                    appointmentId = bookingId,
                    appointmentDate = date,
                )
                val slots = response.dataList
                    .filter { it.timesId.isNotBlank() }
                    .sortedBy { it.startTime }
                    .toPersistentList()
                _uiState.update { s ->
                    val booking = s.booking
                    if (booking == null || booking.selectedDate != date) s
                    else s.copy(
                        booking = booking.copy(
                            timeSlots = slots,
                            loadingTimes = false,
                            loadedTimes = true,
                        ),
                    )
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEAppoint", "加载时段失败", ex)
                _uiState.update { s ->
                    val booking = s.booking ?: return@update s
                    s.copy(
                        booking = booking.copy(
                            loadingTimes = false,
                            loadedTimes = true,
                            error = ex.toUserFacingMessage(UserFacingErrorKind.LoadPEAppointments),
                        ),
                    )
                }
            }
        }
    }

    fun consumeActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun confirmCancel() {
        val target = _uiState.value.cancelTarget ?: return
        if (target.temporaryId.isBlank()) {
            _uiState.update {
                it.copy(cancelTarget = null, actionMessage = "缺少预约标识，无法取消")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(actionInFlight = true) }
            try {
                val response = cancelAppointmentUseCase(target.temporaryId)
                _uiState.update {
                    it.copy(
                        cancelTarget = null,
                        actionInFlight = false,
                        actionMessage = response.message.ifBlank { "已取消预约" },
                    )
                }
                loadMyAppointments()
                loadAvailableAppointments()
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEAppoint", "取消预约失败", ex)
                _uiState.update {
                    it.copy(
                        cancelTarget = null,
                        actionInFlight = false,
                        actionMessage = ex.toUserFacingMessage(UserFacingErrorKind.PEAppointmentAction),
                    )
                }
            }
        }
    }

    fun confirmEnter() {
        val booking = _uiState.value.booking ?: return
        val appointmentId = booking.item.appointmentId.ifBlank {
            booking.detail?.appointmentId.orEmpty()
        }
        val timesId = booking.selectedTimesId
        val enterDate = booking.selectedDate
        when {
            appointmentId.isBlank() -> {
                _uiState.update {
                    it.copy(booking = null, actionMessage = "缺少场次标识，无法预约")
                }
            }
            enterDate.isNullOrBlank() -> {
                _uiState.update {
                    it.copy(booking = booking.copy(error = "请先选择预约日期"))
                }
            }
            timesId.isNullOrBlank() -> {
                _uiState.update {
                    it.copy(booking = booking.copy(error = "请先选择预约时段"))
                }
            }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(actionInFlight = true) }
                    try {
                        val response = enterAppointmentUseCase(
                            appointmentId = appointmentId,
                            timesId = timesId,
                            enterDate = enterDate,
                        )
                        _uiState.update {
                            it.copy(
                                booking = null,
                                actionInFlight = false,
                                actionMessage = response.message.ifBlank { "预约成功" },
                            )
                        }
                        loadMyAppointments()
                        loadAvailableAppointments()
                    } catch (ex: Exception) {
                        if (ex is CancellationException) throw ex
                        AppLogger.log("PEAppoint", "提交预约失败", ex)
                        _uiState.update { s ->
                            s.copy(
                                actionInFlight = false,
                                booking = s.booking?.copy(
                                    error = ex.toUserFacingMessage(UserFacingErrorKind.PEAppointmentAction),
                                ) ?: s.booking,
                                actionMessage = if (s.booking == null) {
                                    ex.toUserFacingMessage(UserFacingErrorKind.PEAppointmentAction)
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TYPE_MY = "1"
        const val TYPE_AVAILABLE = "2"
    }
}

/** 状态文案 */
fun appointmentStatusText(status: String): String = when (status) {
    "0" -> "已取消"
    "1" -> "已预约"
    "2" -> "已完成"
    "3" -> "可预约"
    "4" -> "已截止"
    else -> status.ifBlank { "未知" }
}

/** 是否处于可取消窗口（对齐原页面 nowBtn） */
fun canCancelAppointment(item: PEAppointmentItem, nowMillis: Long = currentTimeMillisCompat()): Boolean {
    if (item.appointmentStatus != "1") return false
    if (item.temporaryId.isBlank()) return false
    val start = parseDateTimeMillis(item.enterStartTime) ?: return true
    val end = parseDateTimeMillis(item.enterEndTime) ?: return true
    return nowMillis in start..end
}

/** 是否可预约（对齐原页面 status==3 才显示预约按钮） */
fun canEnterAppointment(item: PEAppointmentItem): Boolean =
    item.appointmentStatus == "3" && item.appointmentId.isNotBlank()

private fun parseDateTimeMillis(value: String): Long? {
    if (value.isBlank()) return null
    return try {
        val normalized = value.replace('/', '-')
        LocalDateTime.parse(normalized.replace(' ', 'T'))
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    } catch (_: Exception) {
        null
    }
}

private fun currentTimeMillisCompat(): Long =
    kotlin.time.Clock.System.now().toEpochMilliseconds()
