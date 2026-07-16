package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.domain.port.SettingsPort

class PersonalInfoViewModel(
    private val settingsPort: SettingsPort,
) : ViewModel() {
    data class UiState(
        val avatarUri: String? = null,
        val nickname: String? = null,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        loadPersonalInfo()
    }

    private fun loadPersonalInfo() {
        viewModelScope.launch {
            val avatarUri = settingsPort.getUserAvatarUri()
            val nickname = settingsPort.getUserNickname()
            _uiState.value = UiState(avatarUri = avatarUri, nickname = nickname)
        }
    }

    fun saveAvatar(uri: String) {
        viewModelScope.launch {
            settingsPort.setUserAvatarUri(uri)
            loadPersonalInfo()
        }
    }

    fun saveNickname(nickname: String) {
        viewModelScope.launch {
            settingsPort.setUserNickname(nickname)
            loadPersonalInfo()
        }
    }

    fun clearAvatar() {
        viewModelScope.launch {
            settingsPort.setUserAvatarUri(null)
            loadPersonalInfo()
        }
    }
}
