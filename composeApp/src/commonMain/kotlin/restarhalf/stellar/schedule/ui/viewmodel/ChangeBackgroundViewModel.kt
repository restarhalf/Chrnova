package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ChangeBackgroundViewModel : ViewModel() {

    data class ChangeBackgroundUiState(
        val hasCustomImage: Boolean,
        val imageSummary: String,
        val backgroundAlphaPercent: String,
        val backgroundBlurPercent: String,
        val componentsAlphaPercent: String,
    )

    private val _backgroundImageUri = MutableStateFlow<String?>(null)
    private val _backgroundAlpha = MutableStateFlow(1f)
    private val _backgroundBlur = MutableStateFlow(0f)
    private val _componentsAlpha = MutableStateFlow(1f)

    private val _uiState: StateFlow<ChangeBackgroundUiState> =
        combine(
            _backgroundImageUri,
            _backgroundAlpha,
            _backgroundBlur,
            _componentsAlpha,
        ) { backgroundImageUri, backgroundAlpha, backgroundBlur, componentsAlpha ->
            buildScreenUi(
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
                    buildScreenUi(
                        backgroundImageUri = null,
                        backgroundAlpha = 1f,
                        backgroundBlur = 0f,
                        componentsAlpha = 1f,
                    ),
            )

    val uiState: StateFlow<ChangeBackgroundUiState> = _uiState

    fun updateBackground(
        backgroundImageUri: String?,
        backgroundAlpha: Float,
        backgroundBlur: Float,
        componentsAlpha: Float,
    ) {
        _backgroundImageUri.value = backgroundImageUri
        _backgroundAlpha.value = backgroundAlpha
        _backgroundBlur.value = backgroundBlur
        _componentsAlpha.value = componentsAlpha
    }

    fun buildScreenUi(
        backgroundImageUri: String?,
        backgroundAlpha: Float,
        backgroundBlur: Float,
        componentsAlpha: Float,
    ): ChangeBackgroundUiState {
        val hasCustomImage = !backgroundImageUri.isNullOrBlank()
        return ChangeBackgroundUiState(
            hasCustomImage = hasCustomImage,
            imageSummary = if (hasCustomImage) "当前：自定义背景" else "当前：纯色背景",
            backgroundAlphaPercent = "${(backgroundAlpha * 100).toInt()}%",
            backgroundBlurPercent = "${(backgroundBlur * 100).toInt()}%",
            componentsAlphaPercent = "${(componentsAlpha * 100).toInt()}%",
        )
    }
}
