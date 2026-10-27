package restarhalf.stellar.schedule.reminder.receiver

import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.local.mmkv.MmkvSettingsFactory
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.reminder.NotificationChannels

class MuteActionActivity : Activity() {

    private var handled = false

    override fun onResume() {
        super.onResume()

        if (handled) return
        handled = true

        try {
            when (intent.getStringExtra(EXTRA_MODE)) {
                MODE_UNMUTE -> restoreRinger()
                MODE_DISABLE_NOTICE -> disableMuteNotice()
                else -> handleMute()
            }
        } catch (e: Exception) {
            AppLogger.log("Reminder", "铃声操作失败", e)
        }

        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE,0, 0)
        else
            overridePendingTransition(0,0)
    }

    private fun restoreRinger() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val restoreMode =
            intent.getIntExtra(EXTRA_RESTORE_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
        audioManager.ringerMode = restoreMode
        toast("课程已结束，已恢复铃声")
        postMuteStateNotice(muted = false)
    }

    private fun handleMute() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previousMode = audioManager.ringerMode
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val startTime = intent.getStringExtra(EXTRA_COURSE_START_TIME).orEmpty()
        val endTime = intent.getStringExtra(EXTRA_COURSE_END_TIME).orEmpty()
        val now = System.currentTimeMillis()
        val startAtMs = MuteAlarmScheduler.parseTodayTimeMs(startTime)
        val endAtMs = MuteAlarmScheduler.parseTodayTimeMs(endTime)

        val accepted =
            when {

                startAtMs != null && now < startAtMs -> {
                    MuteAlarmScheduler.scheduleClassMute(
                        context = this,
                        notificationId = notificationId,
                        startAtMs = startAtMs,
                        startTime = startTime,
                        endTime = endTime,
                    )
                    toast("已设置上课静音，到点自动开启")
                    true
                }

                endAtMs != null && now in (startAtMs ?: now)..endAtMs -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    MuteAlarmScheduler.scheduleAutoUnmute(
                        context = this,
                        notificationId = notificationId,
                        endTime = endTime,
                        restoreMode = previousMode,
                    )
                    toast("当前在上课时段，已开启静音")
                    postMuteStateNotice(muted = true, endTime = endTime)
                    true
                }

                else -> {
                    toast("当前不在上课时段，未开启静音")
                    false
                }
            }

        if (accepted && notificationId != -1) {
            getSystemService(NotificationManager::class.java)?.cancel(notificationId)
        }
    }

    private fun postMuteStateNotice(muted: Boolean, endTime: String = "") {
        val settings = MmkvSettingsFactory().create(SettingsKeys.PREFS_NAME)
        if (!settings.getBoolean(SettingsKeys.MUTE_STATUS_NOTICE_ENABLED, true)) return

        NotificationChannels.ensureAll(this)

        val title = if (muted) "上课啦，静音已开启" else "下课啦，静音已关闭"
        val text =
            if (muted && endTime.isNotBlank()) "$endTime 自动恢复铃声" else "休息一下吧"

        val builder =
            Notification.Builder(this, NotificationChannels.MUTE_STATUS_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setTimeoutAfter(MUTE_NOTICE_TIMEOUT_MS)

        if (muted) {
            val optOut =
                PendingIntent.getActivity(
                    this,
                    MUTE_NOTICE_ID,
                    Intent(this, MuteActionActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(EXTRA_MODE, MODE_DISABLE_NOTICE)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            builder.addAction(Notification.Action.Builder(null, "不再提醒", optOut).build())
        }

        runCatching {
            getSystemService(NotificationManager::class.java)?.notify(MUTE_NOTICE_ID, builder.build())
        }.onFailure { AppLogger.log("Reminder", "静音状态通知发送失败", it) }
    }

    private fun disableMuteNotice() {
        MmkvSettingsFactory().create(SettingsKeys.PREFS_NAME)
            .putBoolean(SettingsKeys.MUTE_STATUS_NOTICE_ENABLED, false)
        getSystemService(NotificationManager::class.java)?.cancel(MUTE_NOTICE_ID)
        toast("已关闭静音状态提醒")
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_COURSE_START_TIME = "course_time"
        private const val EXTRA_COURSE_END_TIME = "course_end_time"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"
        private const val EXTRA_RESTORE_RINGER_MODE = "restore_ringer_mode"
        private const val EXTRA_MODE = "mute_mode"
        private const val MODE_UNMUTE = "unmute"
        private const val MODE_DISABLE_NOTICE = "disable_notice"

        private const val MUTE_NOTICE_ID = 0x4D55
        private const val MUTE_NOTICE_TIMEOUT_MS = 30 * 60 * 1000L

        fun muteIntent(
            context: Context,
            notificationId: Int,
            startTime: String,
            endTime: String,
        ): Intent =
            Intent(context, MuteActionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_COURSE_START_TIME, startTime)
                putExtra(EXTRA_COURSE_END_TIME, endTime)
            }

        fun applyMuteIntent(
            context: Context,
            notificationId: Int,
            startTime: String,
            endTime: String,
        ): Intent =
            muteIntent(context, notificationId, startTime, endTime)

        fun unmuteIntent(context: Context, restoreMode: Int): Intent =
            Intent(context, MuteActionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_MODE, MODE_UNMUTE)
                putExtra(EXTRA_RESTORE_RINGER_MODE, restoreMode)
            }
    }
}
