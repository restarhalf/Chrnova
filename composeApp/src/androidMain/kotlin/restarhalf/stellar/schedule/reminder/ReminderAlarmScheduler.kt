package restarhalf.stellar.schedule.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.coroutines.CancellationException

internal object ReminderAlarmScheduler {

    fun schedule(
        context: Context,
        triggerAtMs: Long,
        pendingIntent: PendingIntent,
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false

        return try {
            // S 之前无精准闹钟权限，setExact* 始终可用
            val canExact =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
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
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent,
                )
                true
            } catch (e2: Exception) {
                if (e2 is CancellationException) throw e2
                false
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    /**
     * 课表边界闹钟（课前提醒 / 上课瞬间 / 勿扰起止）专用 setAlarmClock。
     *
     * setExactAndAllowWhileIdle 受 Doze「每应用每 9 分钟一次」配额限制，高频对账可能把它挤到迟到；
     * setAlarmClock 不受配额，系统会提前退出 Doze。代价是状态栏显示闹钟图标。
     */
    fun scheduleBoundary(
        context: Context,
        triggerAtMs: Long,
        pendingIntent: PendingIntent,
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        val canExact =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
        if (canExact) {
            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMs, null),
                    pendingIntent,
                )
                return true
            } catch (e: SecurityException) {
                // 落到下面的非精准兜底
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            }
        }
        return try {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            false
        }
    }

    fun hasPending(
        context: Context,
        requestCode: Int,
        receiver: Class<*>,
        action: String? = null,
    ): Boolean = findPending(context, requestCode, receiver, action) != null

    fun cancel(context: Context, requestCode: Int, receiver: Class<*>, action: String? = null) {
        val pendingIntent = findPending(context, requestCode, receiver, action) ?: return
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

    private fun findPending(
        context: Context,
        requestCode: Int,
        receiver: Class<*>,
        action: String?,
    ): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, receiver).apply { if (action != null) setAction(action) },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
}
