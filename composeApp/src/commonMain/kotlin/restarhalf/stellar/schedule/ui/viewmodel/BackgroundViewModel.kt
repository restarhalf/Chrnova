package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

class BackgroundViewModel(
    private val backgroundSettings: BackgroundSettingsPort,
) : ViewModel() {

    @Immutable
    data class BackgroundUiState(
        val backgroundImageUri: String?,
        val backgroundAlpha: Float,
        val backgroundBlur: Float,
        val componentsAlpha: Float,
    )

    private val _uiState: StateFlow<BackgroundUiState> =
        combine(
            backgroundSettings.observeBackgroundImageUri(),
            backgroundSettings.observeBackgroundAlpha(),
            backgroundSettings.observeBackgroundBlur(),
            backgroundSettings.observeComponentsAlpha(),
        ) { backgroundImageUri, backgroundAlpha, backgroundBlur, componentsAlpha ->
            BackgroundUiState(
                backgroundImageUri = backgroundImageUri,
                backgroundAlpha = backgroundAlpha,
                backgroundBlur = backgroundBlur,
                componentsAlpha = componentsAlpha,
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = run {
                    BackgroundUiState(
                        backgroundImageUri = backgroundSettings.getBackgroundImageUri(),
                        backgroundAlpha = backgroundSettings.getBackgroundAlpha(),
                        backgroundBlur = backgroundSettings.getBackgroundBlur(),
                        componentsAlpha = backgroundSettings.getComponentsAlpha(),
                    )
                },
            )

    val uiState: StateFlow<BackgroundUiState> = _uiState

    fun onBackgroundImageUriChanged(uri: String?) {
        backgroundSettings.setBackgroundImageUri(uri)
    }

    fun onBackgroundAlphaChanged(value: Float) {
        backgroundSettings.setBackgroundAlpha(value)
    }

    fun onBackgroundBlurChanged(value: Float) {
        backgroundSettings.setBackgroundBlur(value)
    }

    fun onComponentsAlphaChanged(value: Float) {
        backgroundSettings.setComponentsAlpha(value)
    }
}
