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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.PEDetailData
import restarhalf.stellar.schedule.domain.model.PESubjectHistoryItem
import restarhalf.stellar.schedule.domain.model.PEYearScore
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.PEScoreDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreListUseCase
import restarhalf.stellar.schedule.domain.usecase.PESubjectScoreHistoryUseCase
import restarhalf.stellar.schedule.domain.usecase.PEAuthProfileUseCase

class PEViewModel(
    private val peScoreListUseCase: PEScoreListUseCase,
    private val peScoreDetailUseCase: PEScoreDetailUseCase,
    private val peSubjectScoreHistoryUseCase: PESubjectScoreHistoryUseCase,
    private val peAuthProfileUseCase: PEAuthProfileUseCase,
    private val peAuth: PEAuthPort,
    private val peAuthWorkflow: PEAuthWorkflowPort,
) : ViewModel() {

    @Stable
    data class PeUiState(
        val yearScores: ImmutableList<PEYearScore> = persistentListOf(),
        val detailData: PEDetailData? = null,
        val subjectHistory: SubjectHistoryUiState? = null,
        val authProfile: PEAuthProfile? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val loadedScoreList: Boolean = false,
        val loadedDetail: Boolean = false,
    )

    @Stable
    data class SubjectHistoryUiState(
        val subjectId: String = "",
        val subjectName: String = "",
        val unit: String = "",
        val currentResult: String? = null,
        val records: ImmutableList<PESubjectHistoryItem> = persistentListOf(),
        val loading: Boolean = false,
        val loaded: Boolean = false,
    )

    private val _uiState = MutableStateFlow(PeUiState())

    val uiState: StateFlow<PeUiState> = _uiState

    val isLoggedIn: StateFlow<Boolean> = peAuth.observeToken()
        .map { token -> token.isNotBlank() }
        .catch { e ->
            if (e is CancellationException) throw e
            AppLogger.log("PE", "观察token变化异常", e)
            emit(false)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val loadMutex = Mutex()

    init {
        observeCachedScores()
        observeCachedProfile()
    }

    private fun observeCachedScores() {
        viewModelScope.launch {
            try {
                peScoreListUseCase.observeScoreList()
                    .catch { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("PE", "缓存成绩Flow异常", e)
                    }
                    .collect { scores ->
                        _uiState.update { state ->
                            if (state.yearScores.isEmpty()) {
                                state.copy(yearScores = scores.sortedByDescending { it.schoolYear }.toPersistentList())
                            } else state
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.log("PE", "读取缓存成绩失败", e)
            }
        }
    }

    private fun observeCachedProfile() {
        viewModelScope.launch {
            try {
                peAuth.observeProfile()
                    .catch { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("PE", "观察用户档案Flow异常", e)
                    }
                    .collect { profile ->
                        _uiState.update { state ->
                            if (profile.stuName.isNotBlank()) {
                                state.copy(authProfile = profile)
                            } else state
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.log("PE", "读取用户档案失败", e)
            }
        }
    }

    fun observeCachedDetailData(schoolYear: String) {
        viewModelScope.launch {
            try {
                peScoreDetailUseCase.observeDetailData(schoolYear)
                    .catch { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("PE", "缓存详情Flow异常", e)
                    }
                    .collect { detail ->
                        _uiState.update { state ->
                            if (state.detailData == null && detail != null) {
                                state.copy(detailData = detail)
                            } else state
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.log("PE", "读取缓存详情失败", e)
            }
        }
    }

    fun buildStatusText(isDetail: Boolean = false): String? {
        val state = uiState.value
        return when {
            state.error != null -> state.error
            isDetail -> {
                if (state.loadedDetail && state.detailData == null) "暂无体测详情" else null
            }
            else -> {
                if (state.loadedScoreList && state.yearScores.isEmpty()) "暂无体测成绩" else null
            }
        }
    }

    private suspend fun <T> withAuthRetry(
        action: suspend () -> T,
        onSuccess: (T) -> Unit,
        errorKind: UserFacingErrorKind,
        logTag: String = "PE",
    ) {
        try {
            onSuccess(action())
        } catch (ex: Exception) {
            if (ex is CancellationException) throw ex
            AppLogger.log(logTag, "加载失败", ex)
            _uiState.update { s -> s.copy(error = ex.toUserFacingMessage(errorKind)) }
        }
    }

    fun loadScoreList() {
        viewModelScope.launch {
            loadMutex.withLock {
                _uiState.update { it.copy(loading = true, error = null) }
                withAuthRetry(
                    action = { peScoreListUseCase() },
                    onSuccess = { result ->
                        _uiState.update {
                            it.copy(
                                yearScores = result.dataArr.sortedByDescending { s -> s.schoolYear }.toPersistentList(),
                                loadedScoreList = true,
                            )
                        }
                    },
                    errorKind = UserFacingErrorKind.LoadPEScores,
                )
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun loadScoreDetail(schoolYear: String) {
        viewModelScope.launch {
            loadMutex.withLock {
                _uiState.update { it.copy(loading = true, error = null) }
                withAuthRetry(
                    action = { peScoreDetailUseCase(schoolYear) },
                    onSuccess = { result ->
                        _uiState.update { s -> s.copy(detailData = result.data, loadedDetail = true) }
                    },
                    errorKind = UserFacingErrorKind.LoadPEDetail,
                )
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun buildSubjectHistoryStatusText(): String? {
        val state = uiState.value
        val history = state.subjectHistory ?: return null
        return when {
            state.error != null -> state.error
            history.loaded && history.records.isEmpty() -> "暂无${history.subjectName}成绩记录"
            else -> null
        }
    }

    fun loadSubjectHistory(
        schoolYear: String,
        subjectId: String,
        subjectName: String,
        unit: String,
        currentResult: String?,
    ) {
        viewModelScope.launch {
            loadMutex.withLock {
                _uiState.update {
                    it.copy(
                        error = null,
                        subjectHistory = SubjectHistoryUiState(
                            subjectId = subjectId,
                            subjectName = subjectName,
                            unit = unit,
                            currentResult = currentResult,
                            loading = true,
                        ),
                    )
                }
                withAuthRetry(
                    action = { peSubjectScoreHistoryUseCase(schoolYear, subjectId) },
                    onSuccess = { records ->
                        _uiState.update { s ->
                            val history = s.subjectHistory
                            if (history == null || history.subjectId != subjectId) {
                                s
                            } else {
                                s.copy(
                                    subjectHistory = history.copy(
                                        records = records.toPersistentList(),
                                        loading = false,
                                        loaded = true,
                                    )
                                )
                            }
                        }
                    },
                    errorKind = UserFacingErrorKind.LoadPESubjectHistory,
                )

                _uiState.update { s ->
                    val history = s.subjectHistory
                    if (history != null && history.subjectId == subjectId && history.loading) {
                        s.copy(subjectHistory = history.copy(loading = false, loaded = true))
                    } else s
                }
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            withAuthRetry(
                action = { peAuthProfileUseCase() },
                onSuccess = { },
                errorKind = UserFacingErrorKind.LoadPEScores,
            )
        }
    }

    fun logout() {
        peAuthWorkflow.logout()
        _uiState.value = PeUiState()
    }
}
