package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    private val _backgroundImageUri = MutableStateFlow<String?>(null)
    val backgroundImageUri: StateFlow<String?> = _backgroundImageUri.asStateFlow()

    private val _backgroundAlpha = MutableStateFlow(1f)
    val backgroundAlpha: StateFlow<Float> = _backgroundAlpha.asStateFlow()

    private val _backgroundBlur = MutableStateFlow(0f)
    val backgroundBlur: StateFlow<Float> = _backgroundBlur.asStateFlow()

    private val _componentsAlpha = MutableStateFlow(1f)
    val componentsAlpha: StateFlow<Float> = _componentsAlpha.asStateFlow()

    init {
        viewModelScope.launch {
            observeBackgroundImageUri().collect { _backgroundImageUri.value = it }
        }
        viewModelScope.launch { observeBackgroundAlpha().collect { _backgroundAlpha.value = it } }
        viewModelScope.launch { observeBackgroundBlur().collect { _backgroundBlur.value = it } }
        viewModelScope.launch { observeComponentsAlpha().collect { _componentsAlpha.value = it } }
    }

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
