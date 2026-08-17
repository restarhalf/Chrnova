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
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.domain.model.PEProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.PEScoreDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreListUseCase
import restarhalf.stellar.schedule.domain.usecase.PEProfileUseCase

/**
 * 体育成绩ViewModel
 *
 * 管理体育成绩查询页面的UI状态，包括：
 * - 体育成绩列表加载和显示
 * - 体测详情加载
 * - 用户登录和登出
 * - 本地缓存读取
 */
class PEViewModel(
    private val peScoreListUseCase: PEScoreListUseCase,
    private val peScoreDetailUseCase: PEScoreDetailUseCase,
    private val peProfileUseCase: PEProfileUseCase,
    private val peAuth: PEAuthPort,
    private val peAuthWorkflow: PEAuthWorkflowPort,
) : ViewModel() {

    /**
     * 体育成绩UI状态
     *
     * @param yearScores 年度成绩列表
     * @param detailData 体测详情数据
     * @param peProfile 学生信息
     * @param loading 是否正在加载
     * @param error 错误消息
     * @param loadedScoreList 是否已加载成绩列表
     * @param loadedDetail 是否已加载详情数据
     */
    @Stable
    data class PeUiState(
        val yearScores: ImmutableList<PEYearScore> = persistentListOf(),
        val detailData: PEDetailData? = null,
        val peProfile: PEProfile? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val loadedScoreList: Boolean = false,
        val loadedDetail: Boolean = false,
    )

    private val _uiState = MutableStateFlow(PeUiState())

    /** 统一的UI状态流 */
    val uiState: StateFlow<PeUiState> = _uiState

    /** 是否已登录（响应式，由token变化驱动） */
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
                                state.copy(peProfile = profile)
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

    /**
     * 观察本地缓存的详情数据
     *
     * @param schoolYear 学年
     */
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

    /**
     * 构建状态文本
     *
     * @param isDetail 是否为详情页面
     * @return 状态文本，无需显示时返回null
     */
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

    /** 加载成绩列表 */
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

    /**
     * 加载成绩详情
     *
     * @param schoolYear 学年
     */
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

    /** 加载学生信息（结果通过 observeProfile 响应式更新） */
    fun loadProfile() {
        viewModelScope.launch {
            withAuthRetry(
                action = { peProfileUseCase() },
                onSuccess = { },
                errorKind = UserFacingErrorKind.LoadPEScores,
            )
        }
    }

    /** 用户登出，清除会话并重置状态 */
    fun logout() {
        peAuthWorkflow.logout()
        _uiState.value = PeUiState()
    }
}
