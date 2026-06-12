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

class PEViewModel(private val peUseCase: PEUseCase) : ViewModel() {

    data class ScoreScreenStatus(
        val error: String?,
        val needsLogin: Boolean,
        val loaded: Boolean,
        val empty: Boolean,
    )

    data class DetailScreenStatus(
        val error: String?,
        val needsLogin: Boolean,
        val loaded: Boolean,
        val empty: Boolean,
    )

    private val _yearScores = MutableStateFlow<List<PEYearScore>>(emptyList())
    val yearScores: StateFlow<List<PEYearScore>> = _yearScores.asStateFlow()

    private val _detailData = MutableStateFlow<PEDetailData?>(null)
    val detailData: StateFlow<PEDetailData?> = _detailData.asStateFlow()

    private val _studentInfo = MutableStateFlow<PEStudentInfo?>(null)
    val studentInfo: StateFlow<PEStudentInfo?> = _studentInfo.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _needsLogin = MutableStateFlow(false)
    val needsLogin: StateFlow<Boolean> = _needsLogin.asStateFlow()

    private val _loadedScoreList = MutableStateFlow(false)
    private val _loadedDetail = MutableStateFlow(false)

    private val loadMutex = Mutex()

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
        observeCachedScores()
        observeCachedStudentInfo()
    }

    private fun observeCachedScores() {
        viewModelScope.launch {
            try {
                peUseCase.observeScoreList()
                    .catch { /* ignore, will use network data */ }
                    .collect { scores ->
                        if (_yearScores.value.isEmpty()) {
                            _yearScores.value = scores.sortedByDescending { it.schoolYear }
                        }
                    }
            } catch (e: Exception) {
                // Local cache not available, ignore
            }
        }
    }

    private fun observeCachedStudentInfo() {
        viewModelScope.launch {
            try {
                peUseCase.observeStudentInfo()
                    .catch { /* ignore, will use network data */ }
                    .collect { info ->
                        if (_studentInfo.value == null && info != null) {
                            _studentInfo.value = info
                        }
                    }
            } catch (e: Exception) {
                // Local cache not available, ignore
            }
        }
    }

    fun observeCachedDetailData(schoolYear: String) {
        viewModelScope.launch {
            try {
                peUseCase.observeDetailData(schoolYear)
                    .catch { /* ignore, will use network data */ }
                    .collect { detail ->
                        if (_detailData.value == null && detail != null) {
                            _detailData.value = detail
                        }
                    }
            } catch (e: Exception) {
                // Local cache not available, ignore
            }
        }
    }

    fun buildScoreStatusText(status: ScoreScreenStatus): String? {
        return when {
            status.error != null && !status.needsLogin -> status.error
            status.loaded && status.empty -> "暂无体测成绩"
            else -> null
        }
    }

    fun buildDetailStatusText(status: DetailScreenStatus): String? {
        return when {
            status.error != null && !status.needsLogin -> status.error
            status.loaded && status.empty -> "暂无体测详情"
            else -> null
        }
    }

    fun isLoggedIn(): Boolean = peUseCase.isLoggedIn()

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
                    _loadedScoreList.value = true
                }
                _loading.value = false
            }
        }
    }

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
                    _loadedDetail.value = true
                }
                _loading.value = false
            }
        }
    }

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

    fun onLoginDialogDismissed() {
        _needsLogin.value = false
    }
}
