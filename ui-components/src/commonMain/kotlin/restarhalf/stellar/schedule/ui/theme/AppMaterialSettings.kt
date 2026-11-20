package restarhalf.stellar.schedule.ui.theme

import com.haooz.softglass.glass.BrightTone
import com.haooz.softglass.glass.GlassFill
import com.haooz.softglass.glass.GlassTone
import com.haooz.softglass.glass.SoftGlassStyle
import com.haooz.softglass.glass.ThemeFill
import com.haooz.softglass.platform.isSoftGlassShaderAvailable

/**
 * 玻璃材质口径。
 *
 * 只有两种状态，没有可切换的档位：
 * - 平台能跑柔光 shader（[isSoftGlassShaderAvailable]）→ 柔光玻璃；
 * - 不能 → 无材质，所有玻璃件退回实色，模糊 / 折射 / 边光全关。
 *
 * 判据只在这里实现一次，其余组件一律通过本对象取值，不要再写第二份能力探测。
 */
object AppMaterialSettings {

    /** 玻璃件采样模糊半径（dp）。 */
    private const val GLASS_BLUR_DP = 6f

    /** 弹出面板展开态的采样模糊半径（dp）。 */
    private const val PANEL_BLUR_DP = 12f

    /** 对话框 / 贴底弹窗的采样模糊半径（dp）。 */
    private const val DIALOG_BLUR_DP = 12f

    private const val SURFACE_ALPHA_DROP_LIGHT = 0.2f

    private const val SURFACE_ALPHA_DROP_DARK = 0.32f

    /** 唯一判据：平台能不能跑柔光 shader。 */
    fun glassAvailable(): Boolean = isSoftGlassShaderAvailable()

    /** 玻璃材质总开关；false 时玻璃件不挂模糊与折射，底色改不透明。 */
    fun glassEffectsEnabled(): Boolean = glassAvailable()

    /** 悬浮控件（顶栏按钮、胶囊这类）是否做折射。与 [glassAvailable] 同源，保留语义区分。 */
    fun chromeLensEnabled(): Boolean = glassAvailable()

    fun glassBlurDp(): Float = if (glassAvailable()) GLASS_BLUR_DP else 0f

    fun glassPanelBlurDp(): Float = if (glassAvailable()) PANEL_BLUR_DP else 0f

    fun glassDialogBlurDp(): Float = if (glassAvailable()) DIALOG_BLUR_DP else 0f

    /** 柔光玻璃档的底色 alpha 降幅；无材质档为 0。 */
    fun glassSurfaceAlphaDrop(isLight: Boolean): Float =
        if (!glassAvailable()) {
            0f
        } else if (isLight) {
            SURFACE_ALPHA_DROP_LIGHT
        } else {
            SURFACE_ALPHA_DROP_DARK
        }

    /** 无材质档玻璃面实色化：半透明底色换成不透明，其余情况原样返回。 */
    fun solidSurfaceAlpha(baseAlpha: Float): Float = if (glassAvailable()) baseAlpha else 1f

    /** 玻璃底色 alpha；无材质档改实色，柔光档按 [glassSurfaceAlphaDrop] 降低。 */
    fun glassSurfaceAlpha(milledAlpha: Float, isLight: Boolean): Float =
        if (!glassAvailable()) {
            1f
        } else {
            (milledAlpha - glassSurfaceAlphaDrop(isLight)).coerceAtLeast(0f)
        }

    /**
     * 悬浮控件的玻璃样式：基准样式套上基调与填充。
     *
     * 无材质档把折射与边缘高光一并关掉（纯实色面），其余情况原样返回。
     */
    fun chromeGlassStyle(
        base: SoftGlassStyle,
        isDark: Boolean,
        fill: GlassFill = ThemeFill,
        tone: GlassTone = BrightTone,
    ): SoftGlassStyle {
        val resolved = base.copy(fill = fill, tone = tone).filled(isDark)
        return if (glassAvailable()) {
            resolved
        } else {
            resolved.copy(refractionScale = 0f, edgeHighlight = false)
        }
    }
}
