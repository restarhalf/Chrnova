package restarhalf.stellar.schedule.only.widget

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object WidgetOnStartRefresher {
    private const val WIDGET_REFRESH_THROTTLE_MS = 5_000L

    @Volatile
    private var lastWidgetRefreshAtMs: Long = 0L

    fun refreshIfNeeded(appContext: Context, scope: CoroutineScope) {
        scope.launch {
            val now = SystemClock.elapsedRealtime()
            if (now - lastWidgetRefreshAtMs < WIDGET_REFRESH_THROTTLE_MS) return@launch
            lastWidgetRefreshAtMs = now

            WidgetUpdater.refreshAll(appContext)
            delay(400L)
            WidgetUpdater.refreshAll(appContext)
            WidgetRefreshController.scheduleIfNeeded(appContext)
        }
    }
}
