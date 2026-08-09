package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase

@Immutable
data class StarVerificationState(
    val isVerified: Boolean = false,
    val isVerifying: Boolean = false,
    val showDialog: Boolean = false,
    val username: TextFieldValue = TextFieldValue(),
    val error: String? = null,
)

class StarVerificationHolder(
    private val verifyGitHubStar: VerifyGitHubStarUseCase,
    private val settings: SettingsPort,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(StarVerificationState())
    val state: StateFlow<StarVerificationState> = _state

    init {
        if (settings.getStarVerified()) {
            _state.value = StarVerificationState(isVerified = true)
        }
    }

    fun showDialog() {
        _state.value = _state.value.copy(showDialog = true)
    }

    fun dismissDialog() {
        _state.value = StarVerificationState(
            isVerified = _state.value.isVerified,
            showDialog = false,
            username = TextFieldValue(),
            error = null,
            isVerifying = false,
        )
    }

    fun onUsernameChange(username: TextFieldValue) {
        _state.value = _state.value.copy(username = username)
    }

    fun verify() {
        val username = _state.value.username.text.trim()
        if (username.isBlank()) return

        scope.launch {
            _state.value = _state.value.copy(isVerifying = true, error = null)
            runCatching {
                verifyGitHubStar(username)
            }.onSuccess { starred ->
                _state.value = _state.value.copy(
                    isVerifying = false,
                    isVerified = starred,
                    showDialog = !starred,
                    error = if (!starred) "未检测到 star，请先 star 仓库后再试" else null,
                )
            }.onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("StarVerification", "验证 star 失败", e)
                _state.value = _state.value.copy(
                    isVerifying = false,
                    error = e.message ?: "验证失败"
                )
            }
        }
    }
}
