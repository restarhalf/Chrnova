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
import restarhalf.stellar.schedule.domain.usecase.JwxtLoginUseCase


class JwxtLoginViewModel(
    private val jwxtLoginUseCase: JwxtLoginUseCase,
) : ViewModel() {

    
    @Immutable
    data class JwxtLoginUiState(
        val userNo: String = "",
        val password: String = "",
        val error: String = "",
        val loading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(JwxtLoginUiState())
    val uiState: StateFlow<JwxtLoginUiState> = _uiState.asStateFlow()

    private val loginMutex = Mutex()

    
    fun onUserNoChange(value: String) {
        _uiState.update { it.copy(userNo = value, error = "") }
    }

    
    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = "") }
    }

    
    fun submitLogin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            loginMutex.withLock {
                val current = _uiState.value
                if (current.loading) return@withLock
                val userNo = current.userNo.trim()
                val password = current.password
                if (userNo.isBlank() || password.isBlank()) return@withLock

                _uiState.update { it.copy(loading = true, error = "") }
                runCatching { jwxtLoginUseCase(userNo, password) }
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
