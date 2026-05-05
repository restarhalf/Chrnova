package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import restarhalf.stellar.schedule.domain.usecase.ObserveBackgroundAlphaUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveBackgroundBlurUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveBackgroundImageUriUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveComponentsAlphaUseCase
import restarhalf.stellar.schedule.domain.usecase.SetBackgroundAlphaUseCase
import restarhalf.stellar.schedule.domain.usecase.SetBackgroundBlurUseCase
import restarhalf.stellar.schedule.domain.usecase.SetBackgroundImageUriUseCase
import restarhalf.stellar.schedule.domain.usecase.SetComponentsAlphaUseCase

class BackgroundViewModel(
    private val observeBackgroundImageUri: ObserveBackgroundImageUriUseCase,
    private val observeBackgroundAlpha: ObserveBackgroundAlphaUseCase,
    private val observeBackgroundBlur: ObserveBackgroundBlurUseCase,
    private val observeComponentsAlpha: ObserveComponentsAlphaUseCase,
    private val setBackgroundImageUriUseCase: SetBackgroundImageUriUseCase,
    private val setBackgroundAlphaUseCase: SetBackgroundAlphaUseCase,
    private val setBackgroundBlurUseCase: SetBackgroundBlurUseCase,
    private val setComponentsAlphaUseCase: SetComponentsAlphaUseCase,
) : ViewModel() {

    data class BackgroundUiState(
        val backgroundImageUri: String?,
        val backgroundAlpha: Float,
        val backgroundBlur: Float,
        val componentsAlpha: Float,
    )

    private val _uiState: StateFlow<BackgroundUiState> =
        combine(
            observeBackgroundImageUri(),
            observeBackgroundAlpha(),
            observeBackgroundBlur(),
            observeComponentsAlpha(),
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
                initialValue =
                    BackgroundUiState(
                        backgroundImageUri = null,
                        backgroundAlpha = 1f,
                        backgroundBlur = 0f,
                        componentsAlpha = 1f,
                    ),
            )

    val uiState: StateFlow<BackgroundUiState> = _uiState

    fun setBackgroundImageUri(uri: String?) {
        setBackgroundImageUriUseCase.invoke(uri)
    }

    fun setBackgroundAlpha(value: Float) {
        setBackgroundAlphaUseCase.invoke(value)
    }

    fun setBackgroundBlur(value: Float) {
        setBackgroundBlurUseCase.invoke(value)
    }

    fun setComponentsAlpha(value: Float) {
        setComponentsAlphaUseCase.invoke(value)
    }
}
