package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.usecase.PELoginUseCase

/**
 * PE登录UI状态
 */
data class PELoginUiState(
    val username: String = "",
    val password: String = "",
    val error: String? = null,
    val loading: Boolean = false,
)

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

    private val _uiState = MutableStateFlow(PELoginUiState())
    val uiState: StateFlow<PELoginUiState> = _uiState.asStateFlow()

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
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                peLoginUseCase(_uiState.value.username, _uiState.value.password)
            }.onSuccess { response ->
                if (response.status == "PASS") {
                    _uiState.update { it.copy(password = "", loading = false) }
                    onSuccess()
                } else {
                    AppLogger.log(
                        "PE",
                        "体育系统登录失败: ${response.message}",
                        level = AppLogger.Level.ERROR
                    )
                    _uiState.update { it.copy(loading = false, error = response.message) }
                }
            }.onFailure { throwable ->
                val errorMsg = throwable.message ?: "登录失败"
                AppLogger.log("PE", "体育系统登录失败", throwable)
                _uiState.update { it.copy(loading = false, error = errorMsg) }
            }
        }
    }
}
