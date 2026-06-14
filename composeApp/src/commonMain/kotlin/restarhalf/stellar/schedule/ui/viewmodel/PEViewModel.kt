package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEStudentInfo
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.domain.usecase.PEUseCase

/**
 * 体育成绩UI状态
 *
 * @param yearScores 年度成绩列表
 * @param detailData 体测详情数据
 * @param studentInfo 学生信息
 * @param loading 是否正在加载
 * @param error 错误消息
 * @param needsLogin 是否需要登录
 * @param loadedScoreList 是否已加载成绩列表
 * @param loadedDetail 是否已加载详情数据
 */
data class PeUiState(
    val yearScores: List<PEYearScore> = emptyList(),
    val detailData: PEDetailData? = null,
    val studentInfo: PEStudentInfo? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val needsLogin: Boolean = false,
    val loadedScoreList: Boolean = false,
    val loadedDetail: Boolean = false,
)

/**
 * 体育成绩ViewModel
 *
 * 管理体育成绩查询页面的UI状态，包括：
 * - 体育成绩列表加载和显示
 * - 体测详情加载
 * - 用户登录和登出
 * - 本地缓存读取
 */
class PEViewModel(private val peUseCase: PEUseCase) : ViewModel() {

    private val _yearScores = MutableStateFlow<List<PEYearScore>>(emptyList())
    private val _detailData = MutableStateFlow<PEDetailData?>(null)
    private val _studentInfo = MutableStateFlow<PEStudentInfo?>(null)
    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _needsLogin = MutableStateFlow(false)
    private val _loadedScoreList = MutableStateFlow(false)
    private val _loadedDetail = MutableStateFlow(false)

    private val loadMutex = Mutex()

    /** 统一的UI状态流 */
    val uiState: StateFlow<PeUiState> =
        combine(
            _yearScores,
            _detailData,
            _studentInfo,
            _loading,
            _error,
            _needsLogin,
            _loadedScoreList,
            _loadedDetail,
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            PeUiState(
                yearScores = values[0] as List<PEYearScore>,
                detailData = values[1] as PEDetailData?,
                studentInfo = values[2] as PEStudentInfo?,
                loading = values[3] as Boolean,
                error = values[4] as String?,
                needsLogin = values[5] as Boolean,
                loadedScoreList = values[6] as Boolean,
                loadedDetail = values[7] as Boolean,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PeUiState(),
        )

    init {
        observeCachedScores()
        observeCachedStudentInfo()
    }

    private fun observeCachedScores() {
        viewModelScope.launch {
            try {
                peUseCase.observeScoreList()
                    .catch { e -> AppLogger.log("PE", "缓存成绩Flow异常", e) }
                    .collect { scores ->
                        if (_yearScores.value.isEmpty()) {
                            _yearScores.value = scores.sortedByDescending { it.schoolYear }
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
                peUseCase.observeStudentInfo()
                    .catch { e -> AppLogger.log("PE", "缓存学生信息Flow异常", e) }
                    .collect { info ->
                        if (_studentInfo.value == null && info != null) {
                            _studentInfo.value = info
                        }
                    }
            } catch (e: Exception) {
                AppLogger.log("PE", "读取缓存学生信息失败", e)
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
                peUseCase.observeDetailData(schoolYear)
                    .catch { e -> AppLogger.log("PE", "缓存详情Flow异常", e) }
                    .collect { detail ->
                        if (_detailData.value == null && detail != null) {
                            _detailData.value = detail
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
            state.error != null && !state.needsLogin -> state.error
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
    fun isLoggedIn(): Boolean = peUseCase.isLoggedIn()

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @param onSuccess 登录成功回调
     * @param onError 登录失败回调，参数为错误消息
     */
    fun login(username: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _needsLogin.value = false
            runCatching {
                peUseCase.login(username, password)
            }.onSuccess {
                if (it.status == "PASS") {
                    loadScoreList()
                    loadStudentInfo()
                    onSuccess()
                } else {
                    AppLogger.log("PE", "体育系统登录失败: ${it.message}")
                    _error.value = it.message
                    onError(it.message)
                }
            }.onFailure {
                val errorMsg = it.message ?: "登录失败"
                AppLogger.log("PE", "体育系统登录失败", it)
                _error.value = errorMsg
                onError(errorMsg)
            }
            _loading.value = false
        }
    }

    /** 加载成绩列表 */
    fun loadScoreList() {
        viewModelScope.launch {
            loadMutex.withLock {
                _loading.value = true
                _error.value = null
                _needsLogin.value = false
                runCatching {
                    peUseCase.getScoreList()
                }.onSuccess { result ->
                    _yearScores.value = result.dataArr.sortedByDescending { it.schoolYear }
                    _loadedScoreList.value = true
                }.onFailure {
                    if (it is PETokenExpiredException) {
                        _needsLogin.value = true
                    }
                    AppLogger.log("PE", "加载体育成绩失败", it)
                    _error.value = it.toUserFacingMessage(UserFacingErrorKind.LoadPEScores)
                }
                _loading.value = false
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
                _loading.value = true
                _error.value = null
                _needsLogin.value = false
                runCatching {
                    peUseCase.getScoreDetail(schoolYear)
                }.onSuccess {
                    _detailData.value = it.data
                    _loadedDetail.value = true
                }.onFailure {
                    if (it is PETokenExpiredException) {
                        _needsLogin.value = true
                    }
                    AppLogger.log("PE", "加载体测详情失败", it)
                    _error.value = it.toUserFacingMessage(UserFacingErrorKind.LoadPEDetail)
                }
                _loading.value = false
            }
        }
    }

    /** 加载学生信息 */
    fun loadStudentInfo() {
        viewModelScope.launch {
            _needsLogin.value = false
            runCatching {
                peUseCase.getStudentInfo()
            }.onSuccess {
                _studentInfo.value = it.data
            }.onFailure {
                if (it is PETokenExpiredException) {
                    _needsLogin.value = true
                }
                AppLogger.log("PE", "加载学生信息失败", it)
                _error.value = it.toUserFacingMessage(UserFacingErrorKind.LoadPEScores)
            }
        }
    }

    /** 用户登出，清除所有数据 */
    fun logout() {
        viewModelScope.launch {
            peUseCase.logout()
            _yearScores.value = emptyList()
            _detailData.value = null
            _studentInfo.value = null
            _needsLogin.value = false
            _error.value = null
            _loadedScoreList.value = false
            _loadedDetail.value = false
        }
    }

    /** 登录对话框关闭回调 */
    fun onLoginDialogDismissed() {
        _needsLogin.value = false
    }
}
