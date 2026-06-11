package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEStudentInfo
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.domain.usecase.PEUseCase

class PEViewModel(private val peUseCase: PEUseCase) : ViewModel() {

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
            _loading.value = true
            _error.value = null
            _needsLogin.value = false
            runCatching {
                peUseCase.getScoreList()
            }.onSuccess { it ->
                _yearScores.value = it.dataArr.sortedByDescending { it.schoolYear }
            }.onFailure {
                if (it is PETokenExpiredException) {
                    _needsLogin.value = true
                }
                _error.value = it.message ?: "加载失败"
            }
            _loading.value = false
        }
    }

    fun loadScoreDetail(schoolYear: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _needsLogin.value = false
            runCatching {
                peUseCase.getScoreDetail(schoolYear)
            }.onSuccess {
                _detailData.value = it.data
            }.onFailure {
                if (it is PETokenExpiredException) {
                    _needsLogin.value = true
                }
                _error.value = it.message ?: "加载失败"
            }
            _loading.value = false
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
                _error.value = it.message ?: "获取信息失败"
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
        }
    }

    fun onLoginDialogDismissed() {
        _needsLogin.value = false
    }
}
