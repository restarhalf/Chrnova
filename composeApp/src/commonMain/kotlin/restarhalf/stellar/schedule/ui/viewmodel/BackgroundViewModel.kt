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

/**
 * 背景设置ViewModel
 * 
 * 管理应用背景图片相关的UI状态，包括：
 * - 背景图片URI
 * - 背景透明度
 * - 背景模糊度
 * - 组件透明度（控制背景上组件的可见度）
 */
class BackgroundViewModel(
    observeBackgroundImageUri: ObserveBackgroundImageUriUseCase,
    observeBackgroundAlpha: ObserveBackgroundAlphaUseCase,
    observeBackgroundBlur: ObserveBackgroundBlurUseCase,
    observeComponentsAlpha: ObserveComponentsAlphaUseCase,
    private val setBackgroundImageUriUseCase: SetBackgroundImageUriUseCase,
    private val setBackgroundAlphaUseCase: SetBackgroundAlphaUseCase,
    private val setBackgroundBlurUseCase: SetBackgroundBlurUseCase,
    private val setComponentsAlphaUseCase: SetComponentsAlphaUseCase,
) : ViewModel() {

    /**
     * 背景UI状态
     * 
     * @param backgroundImageUri 背景图片URI，null表示无背景
     * @param backgroundAlpha 背景透明度（0.0-1.0）
     * @param backgroundBlur 背景模糊度（0.0-1.0）
     * @param componentsAlpha 组件透明度（0.0-1.0）
     */
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

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<BackgroundUiState> = _uiState

    /**
     * 背景图片URI变更回调
     * 
     * @param uri 图片URI，null表示移除背景
     */
    fun onBackgroundImageUriChanged(uri: String?) {
        setBackgroundImageUriUseCase.invoke(uri)
    }

    /**
     * 背景透明度变更回调
     * 
     * @param value 透明度（0.0-1.0）
     */
    fun onBackgroundAlphaChanged(value: Float) {
        setBackgroundAlphaUseCase.invoke(value)
    }

    /**
     * 背景模糊度变更回调
     * 
     * @param value 模糊度（0.0-1.0）
     */
    fun onBackgroundBlurChanged(value: Float) {
        setBackgroundBlurUseCase.invoke(value)
    }

    /**
     * 组件透明度变更回调
     * 
     * @param value 透明度（0.0-1.0）
     */
    fun onComponentsAlphaChanged(value: Float) {
        setComponentsAlphaUseCase.invoke(value)
    }
}
