package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * 更换背景ViewModel
 * 
 * 管理背景更换页面的UI状态，包括：
 * - 背景图片选择
 * - 背景透明度调整
 * - 背景模糊度调整
 * - 组件透明度调整
 */
class ChangeBackgroundViewModel : ViewModel() {

    /**
     * 更换背景UI状态
     * 
     * @param hasCustomImage 是否有自定义背景图片
     * @param imageSummary 图片状态摘要
     * @param backgroundAlphaPercent 背景透明度百分比文本
     * @param backgroundBlurPercent 背景模糊度百分比文本
     * @param componentsAlphaPercent 组件透明度百分比文本
     */
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

    /** 对外暴露的UI状态流 */
    val uiState: StateFlow<ChangeBackgroundUiState> = _uiState

    /**
     * 更新背景设置
     * 
     * @param backgroundImageUri 背景图片URI
     * @param backgroundAlpha 背景透明度（0.0-1.0）
     * @param backgroundBlur 背景模糊度（0.0-1.0）
     * @param componentsAlpha 组件透明度（0.0-1.0）
     */
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

    /**
     * 构建更换背景页面UI
     * 
     * @param backgroundImageUri 背景图片URI
     * @param backgroundAlpha 背景透明度
     * @param backgroundBlur 背景模糊度
     * @param componentsAlpha 组件透明度
     * @return 更换背景页面UI
     */
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
