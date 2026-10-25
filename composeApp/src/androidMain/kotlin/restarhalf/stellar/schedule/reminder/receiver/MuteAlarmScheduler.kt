package restarhalf.stellar.schedule.reminder.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import java.util.Calendar

/**
 * 上课静音的闹钟调度。
 *
 * 闹钟目标一律是 [MuteActionActivity] 而不是广播 —— 到点时进程通常在后台，
 * 后台改铃声会被 MIUI 的 AudioHardening 忽略，必须由 Activity 把进程拉到前台。
 */
internal object MuteAlarmScheduler {

    private const val FALLBACK_UNMUTE_DELAY_MS = 90 * 60 * 1000L
    private const val MUTE_REQUEST_CODE_MASK = 0x4D2A
    private const val UNMUTE_REQUEST_CODE_MASK = 0x1F3A

    /** 到开课时间自动静音。 */
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

    /** 下课时恢复铃声。 */
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

    /** 当天 HH:mm 对应的本地时刻；解析不出来返回 null。 */
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
