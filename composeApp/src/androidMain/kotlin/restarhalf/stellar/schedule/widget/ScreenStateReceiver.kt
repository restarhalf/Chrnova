package restarhalf.stellar.schedule.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * 对齐小爱 WidgetUIUpdateReceiver 注册的 TIME_TICK / SCREEN_ON：
 * 亮屏和每分钟 tick 都尝试刷新，节流与熄屏判断放在 [WidgetRefreshController] 里。
 */
class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // 熄屏不刷新；兜底闹钟保留，30 分钟后自然会追上
            }

            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_TIME_TICK -> {
                WidgetRefreshController.refreshFromSystemTick(context)
            }
        }
    }

    companion object {
        fun intentFilter(): IntentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_TIME_TICK)
            }
    }
}
