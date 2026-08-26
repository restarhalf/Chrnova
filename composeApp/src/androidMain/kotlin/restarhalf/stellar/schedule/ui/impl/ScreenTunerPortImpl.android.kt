package restarhalf.stellar.schedule.ui.impl

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.content.ContextWrapper
import restarhalf.stellar.schedule.ui.port.ScreenTunerPort

/**
 * Android 实现：通过当前 Activity 的窗口属性控制。
 *
 * - 常亮：FLAG_KEEP_SCREEN_ON（仅在展示页期间生效，离开即清除）
 * - 亮度：screenBrightness = 1f 覆盖系统亮度，退出时恢复原值
 */
class ScreenTunerPortImpl(
    private val context: Context,
) : ScreenTunerPort {

    private var previousBrightness: Float? = null

    private fun currentActivity(): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    override fun enterScanPresentation(): Boolean {
        val activity = currentActivity() ?: return false
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (previousBrightness == null) {
            val attrs = window.attributes
            val current = attrs.screenBrightness
            if (current != WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                previousBrightness = current
            }
            attrs.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = attrs
        }
        return true
    }

    override fun exitScanPresentation() {
        val activity = currentActivity() ?: return
        val window = activity.window
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val attrs = window.attributes
        attrs.screenBrightness =
            previousBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = attrs
        previousBrightness = null
    }
}
