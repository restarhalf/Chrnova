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

/**
 * 组件刷新调度，对齐小爱课程表的非 MIUI 路径（ua1.c.registerUpdateHelper）：
 *
 *   1. 前台时段靠 TIME_TICK / SCREEN_ON 这类系统广播驱动，同一分钟内只刷一次；
 *   2. 再用 setRepeating 每 30 分钟兜底一次，保证熄屏或广播被拦截时也能追上；
 *   3. 全程不用精确闹钟，所以不依赖 SCHEDULE_EXACT_ALARM。
 *
 * 小爱在检测到 MIUI 组件能力时只走系统曝光刷新、不挂闹钟；但 miuiWidget 元数据
 * 是 MIUI 桌面的白名单能力，第三方包声明后组件会从选择器里消失，所以这里一律走
 * 兜底闹钟这条路。
 */
internal object WidgetRefreshController {
    const val ACTION_WIDGET_FALLBACK_TICK =
        "restarhalf.stellar.schedule.action.WIDGET_FALLBACK_TICK"

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** 兜底刷新周期，取自小爱 setRepeating 的 1800000L */
    private const val FALLBACK_PERIOD_MS = 30L * 60_000L

    private const val FALLBACK_REQUEST_CODE = 10086

    /** 同一分钟内的重复 tick 直接丢弃，对齐小爱 isCurrentMinuteOrSet */
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

    /**
     * 系统广播驱动的一次刷新。返回 true 表示这次真的刷了。
     *
     * 对齐小爱 WidgetUIUpdateReceiver：熄屏时不动，同一分钟内只处理一次。
     */
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

    /** 没有组件时撤掉闹钟，避免空跑唤醒设备 */
    fun scheduleFallback(context: Context) {
        if (!hasAnyPlacedWidget(context)) {
            cancelFallback(context)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
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
