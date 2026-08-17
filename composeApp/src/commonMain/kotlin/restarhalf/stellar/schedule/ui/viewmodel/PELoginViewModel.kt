package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.usecase.PELoginUseCase

/**
 * PE登录UI状态
 */


/**
 * 体育系统登录ViewModel
 *
 * 管理体育系统登录页面的UI状态：
 * - 用户名/密码输入
 * - 登录loading状态
 * - 错误提示
 */
class PELoginViewModel(
    private val peLoginUseCase: PELoginUseCase,
) : ViewModel() {
    @Immutable
    data class PELoginUiState(
        val username: String = "",
        val password: String = "",
        val error: String? = null,
        val loading: Boolean = false,
    )
    private val _uiState = MutableStateFlow(PELoginUiState())
    val uiState: StateFlow<PELoginUiState> = _uiState.asStateFlow()

    private val loginMutex = Mutex()

    /** 用户名输入变更 */
    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, error = null) }
    }

    /** 密码输入变更 */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    /**
     * 提交登录
     *
     * @param onSuccess 登录成功回调
     */
    fun submitLogin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            loginMutex.withLock {
                val current = _uiState.value
                if (current.loading) return@withLock
                val username = current.username.trim()
                val password = current.password
                if (username.isBlank() || password.isBlank()) return@withLock

                _uiState.update { it.copy(loading = true, error = null) }
                runCatching {
                    peLoginUseCase(username, password)
                }.onSuccess {
                    _uiState.update { it.copy(password = "", loading = false) }
                    onSuccess()
                }.onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    val errorMsg = throwable.message ?: "登录失败"
                    AppLogger.log("PEAuth", "体育系统登录失败", throwable)
                    _uiState.update { it.copy(loading = false, error = errorMsg) }
                }
            }
        }
    }
}
