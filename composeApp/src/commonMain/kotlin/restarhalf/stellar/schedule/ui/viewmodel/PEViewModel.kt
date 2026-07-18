package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEStudentInfo
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.usecase.PELoginUseCase
import restarhalf.stellar.schedule.domain.usecase.PELogoutUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreListUseCase
import restarhalf.stellar.schedule.domain.usecase.PEStudentInfoUseCase

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
    private val peLoginUseCase: PELoginUseCase,
    private val peLogoutUseCase: PELogoutUseCase,
    private val peScoreListUseCase: PEScoreListUseCase,
    private val peScoreDetailUseCase: PEScoreDetailUseCase,
    private val peStudentInfoUseCase: PEStudentInfoUseCase,
    private val peAuth: PEAuthPort,
) : ViewModel() {

    /**
     * 体育成绩UI状态
     *
     * @param yearScores 年度成绩列表
     * @param detailData 体测详情数据
     * @param studentInfo 学生信息
     * @param loading 是否正在加载
     * @param error 错误消息
     * @param loadedScoreList 是否已加载成绩列表
     * @param loadedDetail 是否已加载详情数据
     */
    @Stable
    data class PeUiState(
        val yearScores: List<PEYearScore> = emptyList(),
        val detailData: PEDetailData? = null,
        val studentInfo: PEStudentInfo? = null,
        val loading: Boolean = false,
        val error: String? = null,
        val loadedScoreList: Boolean = false,
        val loadedDetail: Boolean = false,
    )

    private val _uiState = MutableStateFlow(PeUiState())

    /** 统一的UI状态流 */
    val uiState: StateFlow<PeUiState> = _uiState

    private val _isLoggedIn = MutableStateFlow(peLoginUseCase.isLoggedIn())

    /** 是否已登录（响应式） */
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val loadMutex = Mutex()

    init {
        observeCachedScores()
        observeCachedStudentInfo()
        observeTokenChanges()
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
                                state.copy(yearScores = scores.sortedByDescending { it.schoolYear })
                            } else state
                        }
                    }
            } catch (e: Exception) {
                AppLogger.log("PE", "读取缓存成绩失败", e)
            }
        }
    }

    private fun observeCachedStudentInfo() {
        viewModelScope.launch {
            try {
                peStudentInfoUseCase.observeStudentInfo()
                    .catch { e ->
                        if (e is CancellationException) throw e
                        AppLogger.log("PE", "缓存学生信息Flow异常", e)
                    }
                    .collect { info ->
                        _uiState.update { state ->
                            if (state.studentInfo == null && info != null) {
                                state.copy(studentInfo = info)
                            } else state
                        }
                    }
            } catch (e: Exception) {
                AppLogger.log("PE", "读取缓存学生信息失败", e)
            }
        }
    }

    private fun observeTokenChanges() {
        viewModelScope.launch {
            peAuth.observeToken()
                .map { token -> !token.isNullOrBlank() }
                .catch { e ->
                    if (e is CancellationException) throw e
                    AppLogger.log("PE", "观察token变化异常", e)
                }
                .collect { loggedIn ->
                    _isLoggedIn.value = loggedIn
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

    /**
     * 检查是否已登录
     *
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean = _isLoggedIn.value

    private suspend fun <T> withAuthRetry(
        action: suspend () -> T,
        onSuccess: (T) -> Unit,
        errorKind: UserFacingErrorKind,
        logTag: String = "PE",
    ) {
        val firstAttempt = runCatching { action() }
        if (firstAttempt.isSuccess) {
            onSuccess(firstAttempt.getOrThrow())
        } else {
            val ex = firstAttempt.exceptionOrNull()!!
            if (ex is CancellationException) throw ex
            if (ex is PETokenExpiredException || ex is SerializationException) {
                val reloginResult = peLoginUseCase.autoLogin()
                if (reloginResult?.status == "PASS") {
                    _isLoggedIn.value = true
                    runCatching { action() }
                        .onSuccess { result -> onSuccess(result) }
                        .onFailure { retryEx ->
                            if (retryEx is CancellationException) throw retryEx
                            AppLogger.log(logTag, "重试失败", retryEx)
                            _isLoggedIn.value = false
                            _uiState.update { s -> s.copy(error = retryEx.toUserFacingMessage(errorKind)) }
                        }
                } else {
                    _isLoggedIn.value = false
                    _uiState.update { s -> s.copy(error = ex.toUserFacingMessage(errorKind)) }
                }
            } else {
                AppLogger.log(logTag, "加载失败", ex)
                _uiState.update { s -> s.copy(error = ex.toUserFacingMessage(errorKind)) }
            }
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
                                yearScores = result.dataArr.sortedByDescending { s -> s.schoolYear },
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

    /** 加载学生信息 */
    fun loadStudentInfo() {
        viewModelScope.launch {
            withAuthRetry(
                action = { peStudentInfoUseCase() },
                onSuccess = { result ->
                    _uiState.update { s -> s.copy(studentInfo = result.data) }
                },
                errorKind = UserFacingErrorKind.LoadPEScores,
            )
        }
    }

    /** 用户登出，清除所有数据 */
    fun logout() {
        viewModelScope.launch {
            peLogoutUseCase()
            _isLoggedIn.value = false
            _uiState.value = PeUiState()
        }
    }
}