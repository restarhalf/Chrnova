package restarhalf.stellar.schedule.ui.viewmodel

import androidx.lifecycle.ViewModel

class ChangeBackgroundViewModel : ViewModel() {

    data class ScreenUi(
        val hasCustomImage: Boolean,
        val imageSummary: String,
        val backgroundAlphaPercent: String,
        val backgroundBlurPercent: String,
        val componentsAlphaPercent: String,
    )

    fun buildScreenUi(
        backgroundImageUri: String?,
        backgroundAlpha: Float,
        backgroundBlur: Float,
        componentsAlpha: Float,
    ): ScreenUi {
        val hasCustomImage = !backgroundImageUri.isNullOrBlank()
        return ScreenUi(
            hasCustomImage = hasCustomImage,
            imageSummary = if (hasCustomImage) "当前：自定义背景" else "当前：纯色背景",
            backgroundAlphaPercent = "${(backgroundAlpha * 100).toInt()}%",
            backgroundBlurPercent = "${(backgroundBlur * 100).toInt()}%",
            componentsAlphaPercent = "${(componentsAlpha * 100).toInt()}%",
        )
    }
}
