package restarhalf.stellar.schedule.reminder.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import restarhalf.stellar.schedule.core.log.AppLogger
import java.util.Calendar

internal object MuteAlarmScheduler {

    private const val FALLBACK_UNMUTE_DELAY_MS = 90 * 60 * 1000L
    private const val MUTE_REQUEST_CODE_MASK = 0x4D2A
    private const val UNMUTE_REQUEST_CODE_MASK = 0x1F3A

    fun scheduleClassMute(
        context: Context,
        notificationId: Int,
        startAtMs: Long,
        endTime: String,
    ) {
        val pendingIntent =
            broadcastPendingIntent(
                context = context,
                requestCode = notificationId xor MUTE_REQUEST_CODE_MASK,
                intent =
                    MuteAlarmReceiver.muteAlarmIntent(
                        context = context,
                        notificationId = notificationId,
                        endTime = endTime,
                    ),
            )
        setAlarmBestEffort(context, startAtMs, pendingIntent)
    }

    fun scheduleAutoUnmute(
        context: Context,
        notificationId: Int,
        endTime: String,
        restoreMode: Int,
    ) {
        val triggerAtMs = parseTodayTimeMs(endTime)
        val pendingIntent =
            broadcastPendingIntent(
                context = context,
                requestCode = notificationId xor UNMUTE_REQUEST_CODE_MASK,
                intent = MuteAlarmReceiver.unmuteAlarmIntent(context, restoreMode),
            )

        val now = System.currentTimeMillis()
        val safeTrigger =
            when {
                triggerAtMs == null -> now + FALLBACK_UNMUTE_DELAY_MS
                triggerAtMs <= now -> now + 5000L
                else -> triggerAtMs
            }
        setAlarmBestEffort(context, safeTrigger, pendingIntent)
    }

    private fun broadcastPendingIntent(
        context: Context,
        requestCode: Int,
        intent: Intent,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun setAlarmBestEffort(
        context: Context,
        triggerAtMs: Long,
        pendingIntent: PendingIntent,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        try {
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
        } catch (e: SecurityException) {
            AppLogger.log("Reminder", "设置静音闹钟SecurityException", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent,
                )
            } catch (e2: Exception) {
                if (e2 is kotlinx.coroutines.CancellationException) throw e2
                AppLogger.log("Reminder", "设置静音闹钟重试失败", e2)
            }
        }
    }

    fun parseTodayTimeMs(time: String): Long? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        val calendar =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        return calendar.timeInMillis
    }
}
