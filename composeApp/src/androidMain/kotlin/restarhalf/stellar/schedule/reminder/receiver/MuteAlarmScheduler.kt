package restarhalf.stellar.schedule.reminder.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import java.util.Calendar

internal object MuteAlarmScheduler {

    private const val FALLBACK_UNMUTE_DELAY_MS = 90 * 60 * 1000L
    private const val MUTE_REQUEST_CODE_MASK = 0x4D2A
    private const val UNMUTE_REQUEST_CODE_MASK = 0x1F3A

    fun scheduleClassMute(
        context: Context,
        notificationId: Int,
        startAtMs: Long,
        startTime: String,
        endTime: String,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent =
            activityPendingIntent(
                context = context,
                requestCode = notificationId xor MUTE_REQUEST_CODE_MASK,
                intent =
                    MuteActionActivity.applyMuteIntent(
                        context = context,
                        notificationId = notificationId,
                        startTime = startTime,
                        endTime = endTime,
                    ),
            )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startAtMs, pendingIntent)
    }

    fun scheduleAutoUnmute(
        context: Context,
        notificationId: Int,
        endTime: String,
        restoreMode: Int,
    ) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val triggerAtMs = parseTodayTimeMs(endTime)
        val pendingIntent =
            activityPendingIntent(
                context = context,
                requestCode = notificationId xor UNMUTE_REQUEST_CODE_MASK,
                intent = MuteActionActivity.unmuteIntent(context, restoreMode),
            )

        val now = System.currentTimeMillis()
        val safeTrigger =
            when {
                triggerAtMs == null -> now + FALLBACK_UNMUTE_DELAY_MS
                triggerAtMs <= now -> now + 5000L
                else -> triggerAtMs
            }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pendingIntent)
    }

    private fun activityPendingIntent(
        context: Context,
        requestCode: Int,
        intent: android.content.Intent,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

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
