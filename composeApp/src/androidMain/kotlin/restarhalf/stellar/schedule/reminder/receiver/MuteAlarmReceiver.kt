package restarhalf.stellar.schedule.reminder.receiver

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import restarhalf.stellar.schedule.MainActivity
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.reminder.NotificationChannels
import restarhalf.stellar.schedule.reminder.ReminderKind

class MuteAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.getStringExtra(EXTRA_MODE)) {
                MODE_UNMUTE -> restoreRinger(context, intent)
                else -> applyMute(context, intent)
            }
        } catch (e: Exception) {
            AppLogger.log("Reminder", "铃声操作失败", e)
        }
    }

    private fun applyMute(context: Context, intent: Intent) {
        if (!RingerControl.canChange(context)) {
            post(context, "未能自动静音", "缺少勿扰权限，请到设置里允许后重新点一次静音")
            return
        }

        val kind = readKind(intent)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previousMode = audioManager.ringerMode
        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val endTime = intent.getStringExtra(EXTRA_COURSE_END_TIME).orEmpty()
        if (endTime.isNotBlank()) {
            MuteAlarmScheduler.scheduleAutoUnmute(
                context = context,
                kind = kind,
                notificationId = notificationId,
                endTime = endTime,
                restoreMode = previousMode,
            )
        }

        if (notificationId != -1) {
            context.getSystemService(NotificationManager::class.java)?.cancel(notificationId)
        }

        post(
            context = context,
            title = "已开启${kind.sessionLabel}静音",
            text = if (endTime.isBlank()) "${kind.endHint}请手动恢复铃声" else "$endTime 自动恢复铃声",
        )
    }

    private fun restoreRinger(context: Context, intent: Intent) {
        val kind = readKind(intent)

        if (!RingerControl.canChange(context)) {
            post(context, "${kind.nounLabel}已结束", "缺少勿扰权限，请手动恢复铃声")
            return
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val restoreMode =
            intent.getIntExtra(EXTRA_RESTORE_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
        audioManager.ringerMode = restoreMode
        post(context, "${kind.nounLabel}已结束", "已恢复铃声")
    }

    private fun readKind(intent: Intent): ReminderKind =
        ReminderKind.fromName(intent.getStringExtra(EXTRA_KIND))

    private fun post(context: Context, title: String, text: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        NotificationChannels.ensureAll(context)

        val contentIntent =
            PendingIntent.getActivity(
                context,
                FEEDBACK_REQUEST_CODE,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notification =
            Notification.Builder(context, NotificationChannels.MUTE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(FEEDBACK_TIMEOUT_MS)
                .build()

        manager.notify(FEEDBACK_NOTIFICATION_ID, notification)
    }

    companion object {

        private const val EXTRA_MODE = "mute_mode"
        private const val MODE_UNMUTE = "unmute"
        private const val EXTRA_KIND = "reminder_kind"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_COURSE_END_TIME = "course_end_time"
        private const val EXTRA_RESTORE_RINGER_MODE = "restore_ringer_mode"

        private const val FEEDBACK_NOTIFICATION_ID = 0x4D2A01
        private const val FEEDBACK_REQUEST_CODE = 0x4D2A02
        private const val FEEDBACK_TIMEOUT_MS = 10 * 60 * 1000L

        fun muteAlarmIntent(
            context: Context,
            kind: ReminderKind,
            notificationId: Int,
            endTime: String,
        ): Intent =
            Intent(context, MuteAlarmReceiver::class.java).apply {
                putExtra(EXTRA_KIND, kind.name)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_COURSE_END_TIME, endTime)
            }

        fun unmuteAlarmIntent(context: Context, kind: ReminderKind, restoreMode: Int): Intent =
            Intent(context, MuteAlarmReceiver::class.java).apply {
                putExtra(EXTRA_MODE, MODE_UNMUTE)
                putExtra(EXTRA_KIND, kind.name)
                putExtra(EXTRA_RESTORE_RINGER_MODE, restoreMode)
            }
    }
}
