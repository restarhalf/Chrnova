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
import restarhalf.stellar.schedule.core.error.UserFacingErrorKind
import restarhalf.stellar.schedule.core.error.toUserFacingMessage
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.usecase.LoginUseCase

/**
 * 教务系统登录ViewModel
 *
 * 管理教务系统登录页面的UI状态：
 * - 用户名/密码输入
 * - 登录loading状态
 * - 错误提示
 */
class JWLoginViewModel(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    /**
     * JW登录UI状态
     */
    @Immutable
    data class JWLoginUiState(
        val userNo: String = "",
        val password: String = "",
        val error: String = "",
        val loading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(JWLoginUiState())
    val uiState: StateFlow<JWLoginUiState> = _uiState.asStateFlow()

    private val loginMutex = Mutex()

    /** 学号输入变更 */
    fun onUserNoChange(value: String) {
        _uiState.update { it.copy(userNo = value, error = "") }
    }

    /** 密码输入变更 */
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = "") }
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
                val userNo = current.userNo.trim()
                val password = current.password
                if (userNo.isBlank() || password.isBlank()) return@withLock

                _uiState.update { it.copy(loading = true, error = "") }
                runCatching { loginUseCase(userNo, password) }
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                password = "",
                                loading = false,
                            )
                        }
                        onSuccess()
                    }
                    .onFailure { throwable ->
                        if (throwable is CancellationException) throw throwable
                        AppLogger.log("Auth", "登录失败 userNo=$userNo", throwable)
                        _uiState.update { state ->
                            state.copy(
                                loading = false,
                                error = throwable.toUserFacingMessage(UserFacingErrorKind.Login)
                            )
                        }
                    }
            }
        }
    }
}
