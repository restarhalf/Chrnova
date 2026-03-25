package restarhalf.stellar.schedule.only.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock

class ScreenStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                WidgetRefreshController.cancelMinuteTick(context)
                WidgetRefreshController.cancelSmallPeriodicTick(context)
            }

            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT -> {
                val now = SystemClock.elapsedRealtime()
                if (now - lastScreenWakeRefreshAtMs < SCREEN_WAKE_REFRESH_THROTTLE_MS) return
                lastScreenWakeRefreshAtMs = now

                if (WidgetRefreshController.hasAnyPlacedWidget(context)) {
                    WidgetRefreshController.refreshAndScheduleAsync(context)
                }
            }
        }
    }

    companion object {
        private const val SCREEN_WAKE_REFRESH_THROTTLE_MS = 2_000L

        @Volatile
        private var lastScreenWakeRefreshAtMs: Long = 0L

        fun intentFilter(): IntentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
    }
}
