package restarhalf.stellar.schedule.widget

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.log.AppLogger
import java.util.Calendar

internal object WidgetRefreshController {
    const val ACTION_WIDGET_MINUTE_TICK = "restarhalf.stellar.schedule.action.WIDGET_MINUTE_TICK"
    const val ACTION_WIDGET_SMALL_PERIODIC_TICK =
        "restarhalf.stellar.schedule.action.WIDGET_SMALL_PERIODIC_TICK"

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private const val ACTIVE_START_HOUR = 8
    private const val ACTIVE_END_HOUR = 22

    private const val SMALL_REFRESH_PERIOD_MS = 50L * 60_000L

    @RequiresApi(Build.VERSION_CODES.S)
    fun refreshAndScheduleAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                WidgetUpdater.refreshAll(appContext)
                scheduleIfNeeded(appContext)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("Widget", "刷新小部件失败", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleIfNeeded(context: Context) {
        val hasLarge = hasLargePlacedWidget(context)
        val hasSmall = hasSmallPlacedWidget(context)

        if (hasLarge) {
            scheduleNextMinuteTick(context)
        } else {
            cancelMinuteTick(context)
        }

        if (hasSmall) {
            scheduleNextSmallPeriodicTick(context)
        } else {
            cancelSmallPeriodicTick(context)
        }
    }

    fun hasAnyPlacedWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val smallIds =
            manager.getAppWidgetIds(ComponentName(context, TodaySmallWidgetReceiver::class.java))
        val largeIds =
            manager.getAppWidgetIds(ComponentName(context, TodayLargeWidgetReceiver::class.java))
        return smallIds.isNotEmpty() || largeIds.isNotEmpty()
    }

    fun hasSmallPlacedWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val smallIds =
            manager.getAppWidgetIds(ComponentName(context, TodaySmallWidgetReceiver::class.java))
        return smallIds.isNotEmpty()
    }

    fun hasLargePlacedWidget(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val largeIds =
            manager.getAppWidgetIds(ComponentName(context, TodayLargeWidgetReceiver::class.java))
        return largeIds.isNotEmpty()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleNextMinuteTick(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMillis = computeNextTriggerAtMillis(System.currentTimeMillis())
        val pendingIntent = buildTickPendingIntent(context)

        try {
            if (!alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            AppLogger.log("Widget", "设置精确闹钟SecurityException", e)
            alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
        }
    }

    private fun computeNextTriggerAtMillis(nowMs: Long): Long {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val hour = nowCal.get(Calendar.HOUR_OF_DAY)

        val inActiveWindow = hour in ACTIVE_START_HOUR until ACTIVE_END_HOUR
        if (inActiveWindow) {
            return ((nowMs / 60_000L) + 1L) * 60_000L + 300L
        }

        val startCal =
            Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, ACTIVE_START_HOUR)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        if (startCal.timeInMillis <= nowMs) {
            startCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return startCal.timeInMillis
    }

    fun cancelMinuteTick(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                10086,
                Intent(context, WidgetRefreshReceiver::class.java).setAction(
                    ACTION_WIDGET_MINUTE_TICK
                ),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelSmallPeriodicTick(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                10087,
                Intent(context, WidgetRefreshReceiver::class.java)
                    .setAction(ACTION_WIDGET_SMALL_PERIODIC_TICK),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildTickPendingIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, WidgetRefreshReceiver::class.java).setAction(ACTION_WIDGET_MINUTE_TICK)
        return PendingIntent.getBroadcast(
            context,
            10086,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleNextSmallPeriodicTick(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMillis = computeNextSmallPeriodicTriggerAtMillis(System.currentTimeMillis())
        val pendingIntent = buildSmallPeriodicPendingIntent(context)
        try {
            if (!alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            AppLogger.log("Widget", "设置小部件精确闹钟SecurityException", e)
            alarmManager.set(AlarmManager.RTC, triggerAtMillis, pendingIntent)
        }
    }

    private fun computeNextSmallPeriodicTriggerAtMillis(nowMs: Long): Long {
        return ((nowMs / SMALL_REFRESH_PERIOD_MS) + 1L) * SMALL_REFRESH_PERIOD_MS + 300L
    }

    private fun buildSmallPeriodicPendingIntent(context: Context): PendingIntent {
        val intent =
            Intent(context, WidgetRefreshReceiver::class.java)
                .setAction(ACTION_WIDGET_SMALL_PERIODIC_TICK)
        return PendingIntent.getBroadcast(
            context,
            10087,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class WidgetRefreshReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isMinuteTick = action == WidgetRefreshController.ACTION_WIDGET_MINUTE_TICK
        val isSmallPeriodicTick =
            action == WidgetRefreshController.ACTION_WIDGET_SMALL_PERIODIC_TICK
        val isSystemRefresh =
            action == Intent.ACTION_TIME_CHANGED ||
                    action == Intent.ACTION_TIMEZONE_CHANGED ||
                    action == Intent.ACTION_DATE_CHANGED ||
                    action == Intent.ACTION_BOOT_COMPLETED ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED

        if (!isMinuteTick && !isSmallPeriodicTick && !isSystemRefresh) return

        val result = goAsync()
        val appContext = context.applicationContext
        WidgetRefreshController.scope.launch {
            try {
                if (isMinuteTick) {
                    WidgetUpdater.refreshLargeOnly(appContext)
                } else if (isSmallPeriodicTick) {
                    WidgetUpdater.refreshSmallOnly(appContext)
                } else {
                    WidgetUpdater.refreshAll(appContext)
                }
                WidgetRefreshController.scheduleIfNeeded(appContext)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("Widget", "刷新小部件失败", e)
            } finally {
                result.finish()
            }
        }
    }
}
