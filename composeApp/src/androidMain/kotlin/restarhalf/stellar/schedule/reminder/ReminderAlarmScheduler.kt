package restarhalf.stellar.schedule.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.core.log.AppLogger

internal object ReminderAlarmScheduler {

    fun schedule(
        context: Context,
        triggerAtMs: Long,
        pendingIntent: PendingIntent,
        logLabel: String,
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false

        return try {
            val canExact =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent,
                )
            }
            true
        } catch (e: SecurityException) {
            AppLogger.log("Reminder", "设置${logLabel}SecurityException", e)

            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent,
                )
                true
            } catch (e2: Exception) {
                if (e2 is CancellationException) throw e2
                AppLogger.log("Reminder", "设置${logLabel}重试失败", e2)
                false
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppLogger.log("Reminder", "设置${logLabel}失败", e)
            false
        }
    }

    fun hasPending(context: Context, requestCode: Int, receiver: Class<*>): Boolean =
        findPending(context, requestCode, receiver) != null

    fun cancel(context: Context, requestCode: Int, receiver: Class<*>) {
        val pendingIntent = findPending(context, requestCode, receiver) ?: return
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun readNextTrigger(settings: ObservableSettings, key: String, nowMs: Long): Long? {
        if (!settings.hasKey(key)) return null
        val triggerAtMs = settings.getLong(key, 0L)
        if (triggerAtMs <= 0L || triggerAtMs <= nowMs) return null
        return triggerAtMs
    }

    fun writeNextTrigger(settings: ObservableSettings, key: String, triggerAtMs: Long) {
        settings[key] = triggerAtMs
    }

    fun clearNextTrigger(settings: ObservableSettings, key: String) {
        settings.remove(key)
    }

    private fun findPending(context: Context, requestCode: Int, receiver: Class<*>): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
}
