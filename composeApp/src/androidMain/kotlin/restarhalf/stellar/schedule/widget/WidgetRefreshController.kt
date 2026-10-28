package restarhalf.stellar.schedule.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.log.AppLogger

internal object WidgetRefreshController {
    const val ACTION_WIDGET_FALLBACK_TICK =
        "restarhalf.stellar.schedule.action.WIDGET_FALLBACK_TICK"

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private const val FALLBACK_PERIOD_MS = 30L * 60_000L

    private const val FALLBACK_REQUEST_CODE = 10086

    @Volatile
    private var lastHandledMinute: Long = -1L

    fun refreshAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                WidgetUpdater.refreshAll(appContext)
                scheduleFallback(appContext)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("Widget", "刷新小部件失败", e)
            }
        }
    }

    fun refreshFromSystemTick(context: Context): Boolean {
        if (!hasAnyPlacedWidget(context)) return false

        val powerManager = context.getSystemService(PowerManager::class.java)
        if (powerManager?.isInteractive == false) return false

        if (!markMinuteHandled(SystemClock.elapsedRealtime())) return false

        refreshAsync(context)
        return true
    }

    private fun markMinuteHandled(elapsedRealtimeMs: Long): Boolean {
        val minute = elapsedRealtimeMs / 60_000L
        if (minute == lastHandledMinute) return false
        lastHandledMinute = minute
        return true
    }

    fun hasAnyPlacedWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val smallIds =
            manager.getAppWidgetIds(ComponentName(context, TodaySmallWidgetReceiver::class.java))
        if (smallIds.isNotEmpty()) return true
        val largeIds =
            manager.getAppWidgetIds(ComponentName(context, TodayLargeWidgetReceiver::class.java))
        return largeIds.isNotEmpty()
    }

    fun scheduleFallback(context: Context) {
        if (!hasAnyPlacedWidget(context)) {
            cancelFallback(context)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // 非唤醒型：小组件只在屏幕亮着时才有意义，没必要每 30 分钟把设备从
        // 睡眠里叫醒一次（一天 48 次）。睡着时这次 tick 会等到下次醒来再补，
        // 亮屏/解锁本来也有 ScreenStateReceiver 兜着。
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1_000L,
            FALLBACK_PERIOD_MS,
            buildFallbackPendingIntent(context),
        )
    }

    fun cancelFallback(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                FALLBACK_REQUEST_CODE,
                Intent(context, WidgetRefreshReceiver::class.java)
                    .setAction(ACTION_WIDGET_FALLBACK_TICK),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildFallbackPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            FALLBACK_REQUEST_CODE,
            Intent(context, WidgetRefreshReceiver::class.java)
                .setAction(ACTION_WIDGET_FALLBACK_TICK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isFallbackTick = action == WidgetRefreshController.ACTION_WIDGET_FALLBACK_TICK
        val isSystemRefresh =
            action == Intent.ACTION_TIME_CHANGED ||
                action == Intent.ACTION_TIMEZONE_CHANGED ||
                action == Intent.ACTION_DATE_CHANGED ||
                action == Intent.ACTION_BOOT_COMPLETED ||
                action == Intent.ACTION_MY_PACKAGE_REPLACED

        if (!isFallbackTick && !isSystemRefresh) return

        val result = goAsync()
        val appContext = context.applicationContext
        WidgetRefreshController.scope.launch {
            try {
                WidgetUpdater.refreshAll(appContext)
                WidgetRefreshController.scheduleFallback(appContext)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("Widget", "刷新小部件失败", e)
            } finally {
                result.finish()
            }
        }
    }
}
