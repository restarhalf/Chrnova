package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

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
    private val backgroundSettings: BackgroundSettingsPort,
) : ViewModel() {

    /**
     * 背景UI状态
     * 
     * @param backgroundImageUri 背景图片URI，null表示无背景
     * @param backgroundAlpha 背景透明度（0.0-1.0）
     * @param backgroundBlur 背景模糊度（0.0-1.0）
     * @param componentsAlpha 组件透明度（0.0-1.0）
     */
    @Immutable
    data class BackgroundUiState(
        val backgroundImageUri: String?,
        val backgroundAlpha: Float,
        val backgroundBlur: Float,
        val componentsAlpha: Float,
    )

    @Immutable
    data class ScreenUi(
        val hasCustomImage: Boolean,
        val imageSummary: String,
        val backgroundAlphaPercent: String,
        val backgroundBlurPercent: String,
        val componentsAlphaPercent: String,
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

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<BackgroundUiState> = _uiState

    /**
     * 背景图片URI变更回调
     * 
     * @param uri 图片URI，null表示移除背景
     */
    fun onBackgroundImageUriChanged(uri: String?) {
        backgroundSettings.setBackgroundImageUri(uri)
    }

    /**
     * 背景透明度变更回调
     * 
     * @param value 透明度（0.0-1.0）
     */
    fun onBackgroundAlphaChanged(value: Float) {
        backgroundSettings.setBackgroundAlpha(value)
    }

    /**
     * 背景模糊度变更回调
     * 
     * @param value 模糊度（0.0-1.0）
     */
    fun onBackgroundBlurChanged(value: Float) {
        backgroundSettings.setBackgroundBlur(value)
    }

    /**
     * 组件透明度变更回调
     * 
     * @param value 透明度（0.0-1.0）
     */
    fun onComponentsAlphaChanged(value: Float) {
        backgroundSettings.setComponentsAlpha(value)
    }
}
