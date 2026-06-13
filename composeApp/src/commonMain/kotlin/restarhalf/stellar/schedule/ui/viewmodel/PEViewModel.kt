package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEStudentInfo
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.domain.usecase.PEUseCase

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

    /**
     * 成绩页面状态
     * 
     * @param error 错误消息
     * @param needsLogin 是否需要登录
     * @param loaded 是否已加载数据
     * @param empty 数据是否为空
     */
    data class ScoreScreenStatus(
        val error: String?,
        val needsLogin: Boolean,
        val loaded: Boolean,
        val empty: Boolean,
    )

    /**
     * 详情页面状态
     * 
     * @param error 错误消息
     * @param needsLogin 是否需要登录
     * @param loaded 是否已加载数据
     * @param empty 数据是否为空
     */
    data class DetailScreenStatus(
        val error: String?,
        val needsLogin: Boolean,
        val loaded: Boolean,
        val empty: Boolean,
    )

    /** 年度成绩列表 */
    private val _yearScores = MutableStateFlow<List<PEYearScore>>(emptyList())
    val yearScores: StateFlow<List<PEYearScore>> = _yearScores.asStateFlow()

    /** 体测详情数据 */
    private val _detailData = MutableStateFlow<PEDetailData?>(null)
    val detailData: StateFlow<PEDetailData?> = _detailData.asStateFlow()

    /** 学生信息 */
    private val _studentInfo = MutableStateFlow<PEStudentInfo?>(null)
    val studentInfo: StateFlow<PEStudentInfo?> = _studentInfo.asStateFlow()

    /** 加载状态 */
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** 错误消息 */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** 是否需要登录 */
    private val _needsLogin = MutableStateFlow(false)
    val needsLogin: StateFlow<Boolean> = _needsLogin.asStateFlow()

    private val _loadedScoreList = MutableStateFlow(false)
    private val _loadedDetail = MutableStateFlow(false)

    /** 加载互斥锁，防止并发请求 */
    private val loadMutex = Mutex()

    /** 成绩页面状态 */
    val scoreScreenStatus: StateFlow<ScoreScreenStatus> =
        combine(_error, _needsLogin, _loadedScoreList, _yearScores) { error, needsLogin, loaded, scores ->
            ScoreScreenStatus(
                error = error,
                needsLogin = needsLogin,
                loaded = loaded,
                empty = scores.isEmpty(),
            )
        }.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = ScoreScreenStatus(error = null, needsLogin = false, loaded = false, empty = true),
        )

    /** 详情页面状态 */
    val detailScreenStatus: StateFlow<DetailScreenStatus> =
        combine(_error, _needsLogin, _loadedDetail, _detailData) { error, needsLogin, loaded, detail ->
            DetailScreenStatus(
                error = error,
                needsLogin = needsLogin,
                loaded = loaded,
                empty = detail == null,
            )
        }.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailScreenStatus(error = null, needsLogin = false, loaded = false, empty = true),
        )

    init {
        // 初始化时加载本地缓存
        observeCachedScores()
        observeCachedStudentInfo()
    }

    /** 观察本地缓存的成绩数据 */
    private fun observeCachedScores() {
        viewModelScope.launch {
            try {
                peUseCase.observeScoreList()
                    .catch { /* 忽略错误，将使用网络数据 */ }
                    .collect { scores ->
                        if (_yearScores.value.isEmpty()) {
                            _yearScores.value = scores.sortedByDescending { it.schoolYear }
                        }
                    }
            } catch (e: Exception) {
                // 本地缓存不可用，忽略
            }
        }
    }

    /** 观察本地缓存的学生信息 */
    private fun observeCachedStudentInfo() {
        viewModelScope.launch {
            try {
                peUseCase.observeStudentInfo()
                    .catch { /* 忽略错误，将使用网络数据 */ }
                    .collect { info ->
                        if (_studentInfo.value == null && info != null) {
                            _studentInfo.value = info
                        }
                    }
            } catch (e: Exception) {
                // 本地缓存不可用，忽略
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
                    .catch { /* 忽略错误，将使用网络数据 */ }
                    .collect { detail ->
                        if (_detailData.value == null && detail != null) {
                            _detailData.value = detail
                        }
                    }
            } catch (e: Exception) {
                // 本地缓存不可用，忽略
            }
        }
    }

    /**
     * 构建成绩页面状态文本
     * 
     * @param status 成绩页面状态
     * @return 状态文本，无需显示时返回null
     */
    fun buildScoreStatusText(status: ScoreScreenStatus): String? {
        return when {
            status.error != null && !status.needsLogin -> status.error
            status.loaded && status.empty -> "暂无体测成绩"
            else -> null
        }
    }

    /**
     * 构建详情页面状态文本
     * 
     * @param status 详情页面状态
     * @return 状态文本，无需显示时返回null
     */
    fun buildDetailStatusText(status: DetailScreenStatus): String? {
        return when {
            status.error != null && !status.needsLogin -> status.error
            status.loaded && status.empty -> "暂无体测详情"
            else -> null
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
                    _error.value = it.message
                    onError(it.message)
                }
            }.onFailure {
                val errorMsg = it.message ?: "登录失败"
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
                }.onSuccess { it ->
                    _yearScores.value = it.dataArr.sortedByDescending { it.schoolYear }
                    _loadedScoreList.value = true
                }.onFailure {
                    if (it is PETokenExpiredException) {
                        _needsLogin.value = true
                    }
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
