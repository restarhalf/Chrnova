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
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.PEAuthStore
import restarhalf.stellar.schedule.data.remote.PECodeItem
import restarhalf.stellar.schedule.data.remote.PEFreeApplyItem
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.usecase.PEFreeApplyUseCase

/**
 * 免测/缓测申请 ViewModel
 *
 * 列表页 + 独立申请页共用。
 */
class PEFreeApplyViewModel(
    private val useCase: PEFreeApplyUseCase,
    private val peAuth: PEAuthPort,
    private val authStore: PEAuthStore,
) : ViewModel() {

    @Stable
    data class AttachmentUi(
        val attId: String,
        val name: String,
        val mimeType: String,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AttachmentUi) return false
            return attId == other.attId && name == other.name
        }

        override fun hashCode(): Int = attId.hashCode() * 31 + name.hashCode()
    }

    @Stable
    data class FreeApplyUiState(
        val loading: Boolean = false,
        val loaded: Boolean = false,
        val items: ImmutableList<PEFreeApplyItem> = persistentListOf(),
        val schoolYears: ImmutableList<PECodeItem> = persistentListOf(),
        val nowSchoolYear: String = "",
        val selectedYearCode: String = "",
        val selectedYearLabel: String = "",
        val freeApplyType: String = "",
        val freeApplyTypeLabel: String = "",
        val attachments: ImmutableList<AttachmentUi> = persistentListOf(),
        val uploading: Boolean = false,
        val submitting: Boolean = false,
        val error: String? = null,
        val actionMessage: String? = null,
        val typeLabelMap: Map<String, String> = emptyMap(),
        val statusLabelMap: Map<String, String> = emptyMap(),
    )

    private val _uiState = MutableStateFlow(FreeApplyUiState())
    val uiState: StateFlow<FreeApplyUiState> = _uiState

    val isLoggedIn: StateFlow<Boolean> = peAuth.observeToken()
        .map { token -> token.isNotBlank() }
        .catch { e ->
            if (e is CancellationException) throw e
            AppLogger.log("PEFree", "观察token异常", e)
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                val listResp = useCase.list()
                val yearResp = runCatching { useCase.schoolYears() }.getOrNull()
                val years = yearResp?.data?.dataList.orEmpty()
                val now = yearResp?.data?.nowSchoolYear.orEmpty()
                val defaultYear = years.firstOrNull { it.code == now } ?: years.firstOrNull()
                _uiState.update {
                    it.copy(
                        loading = false,
                        loaded = true,
                        items = listResp.data.toPersistentList(),
                        schoolYears = years.toPersistentList(),
                        nowSchoolYear = now,
                        selectedYearCode = if (it.selectedYearCode.isBlank()) {
                            defaultYear?.code.orEmpty()
                        } else {
                            it.selectedYearCode
                        },
                        selectedYearLabel = if (it.selectedYearLabel.isBlank()) {
                            defaultYear?.label ?: defaultYear?.code.orEmpty()
                        } else {
                            it.selectedYearLabel
                        },
                    )
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEFree", "加载免测列表失败", ex)
                _uiState.update {
                    it.copy(
                        loading = false,
                        loaded = true,
                        error = ex.toUserFacingMessage(UserFacingErrorKind.LoadPEFreeApply),
                    )
                }
            }
        }
    }

    /** 进入申请页前重置表单并确保学年已加载 */
    fun prepareApplyForm() {
        val year = _uiState.value.let { s ->
            s.schoolYears.firstOrNull { it.code == s.nowSchoolYear }
                ?: s.schoolYears.firstOrNull()
        }
        _uiState.update {
            it.copy(
                error = null,
                attachments = persistentListOf(),
                freeApplyType = "",
                freeApplyTypeLabel = "",
                selectedYearCode = year?.code ?: it.nowSchoolYear,
                selectedYearLabel = (year?.label ?: year?.code ?: it.nowSchoolYear),
            )
        }
        if (_uiState.value.schoolYears.isEmpty() || _uiState.value.nowSchoolYear.isBlank()) {
            refresh()
        }
    }

    fun clearFormError() {
        _uiState.update { it.copy(error = null) }
    }

    fun selectYear(code: String, label: String) {
        _uiState.update {
            it.copy(selectedYearCode = code, selectedYearLabel = label.ifBlank { code })
        }
    }

    fun selectApplyType(code: String, label: String) {
        _uiState.update {
            it.copy(
                freeApplyType = code,
                freeApplyTypeLabel = label.ifBlank { code },
            )
        }
    }

    fun addAttachment(fileName: String, mimeType: String, bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploading = true, error = null) }
            try {
                val resp = useCase.upload(fileName, mimeType, bytes)
                val attId = resp.attId
                    ?: throw IllegalStateException(resp.message.ifBlank { "上传失败" })
                _uiState.update { s ->
                    val next = s.attachments.filterNot { it.attId == attId } +
                        AttachmentUi(attId = attId, name = fileName, mimeType = mimeType)
                    s.copy(uploading = false, attachments = next.toPersistentList())
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) throw ex
                AppLogger.log("PEFree", "上传附件失败", ex)
                _uiState.update {
                    it.copy(
                        uploading = false,
                        error = ex.toUserFacingMessage(UserFacingErrorKind.PEAppointmentAction),
                    )
                }
            }
        }
    }

    fun removeAttachment(attId: String) {
        _uiState.update { s ->
            s.copy(attachments = s.attachments.filterNot { it.attId == attId }.toPersistentList())
        }
    }

    fun consumeActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun submitApply() {
        val state = _uiState.value
        val stdNumber = authStore.getStdNumber().orEmpty()
        when {
            stdNumber.isBlank() -> {
                _uiState.update { it.copy(error = "缺少学号，请先完善体测系统资料") }
            }
            state.selectedYearCode.isBlank() -> {
                _uiState.update { it.copy(error = "请选择申请学年") }
            }
            state.freeApplyType.isBlank() -> {
                _uiState.update { it.copy(error = "请选择申请类型") }
            }
            state.attachments.isEmpty() -> {
                _uiState.update { it.copy(error = "请上传附件") }
            }
            else -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(submitting = true, error = null) }
                    try {
                        val resp = useCase.apply(
                            stdNumber = stdNumber,
                            schoolYear = state.selectedYearCode,
                            freeApplyType = state.freeApplyType,
                            attachments = state.attachments.map { it.attId },
                        )
                        _uiState.update {
                            it.copy(
                                submitting = false,
                                attachments = persistentListOf(),
                                freeApplyType = "",
                                freeApplyTypeLabel = "",
                                actionMessage = resp.message.ifBlank { "申请提交成功" },
                            )
                        }
                        refresh()
                    } catch (ex: Exception) {
                        if (ex is CancellationException) throw ex
                        AppLogger.log("PEFree", "提交免测申请失败", ex)
                        _uiState.update {
                            it.copy(
                                submitting = false,
                                error = ex.toUserFacingMessage(UserFacingErrorKind.PEAppointmentAction),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 申请类型：仅免测 / 缓测 */
val FreeApplyTypes: List<PECodeItem>
    get() = listOf(
        PECodeItem(code = "1", label = "免测"),
        PECodeItem(code = "2", label = "缓测"),
    )

fun freeApplyStatusText(status: String, map: Map<String, String>): String =
    map[status] ?: when (status) {
        "0" -> "待审核"
        "1" -> "审核通过"
        "2" -> "已驳回"
        "3" -> "已撤销"
        else -> status.ifBlank { "未知" }
    }

fun freeApplyTypeText(code: String, map: Map<String, String>): String =
    map[code] ?: FreeApplyTypes.firstOrNull { it.code == code }?.label
        ?: when (code) {
            "1" -> "免测"
            "2" -> "缓测"
            else -> code.ifBlank { "—" }
        }
