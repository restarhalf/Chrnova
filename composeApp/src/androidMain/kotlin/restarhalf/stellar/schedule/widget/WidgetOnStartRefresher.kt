package restarhalf.stellar.schedule.widget

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal object WidgetOnStartRefresher {
    private const val WIDGET_REFRESH_THROTTLE_MS = 5_000L

    @Volatile
    private var lastWidgetRefreshAtMs: Long = 0L

    @RequiresApi(Build.VERSION_CODES.S)
    fun refreshIfNeeded(appContext: Context, scope: CoroutineScope) {
        scope.launch {
            val now = SystemClock.elapsedRealtime()
            if (now - lastWidgetRefreshAtMs < WIDGET_REFRESH_THROTTLE_MS) return@launch
            lastWidgetRefreshAtMs = now

            WidgetUpdater.refreshAll(appContext)
            delay(400L.milliseconds)
            WidgetUpdater.refreshAll(appContext)
            WidgetRefreshController.scheduleIfNeeded(appContext)
        }
    }
}
