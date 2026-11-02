package restarhalf.stellar.schedule.reminder.receiver

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.reminder.ReminderKind
import restarhalf.stellar.schedule.reminder.parseTodayTimeMs

class MuteActionActivity : Activity() {

    private var handled = false

    override fun onResume() {
        super.onResume()

        if (handled) return
        handled = true

        try {
            handleMute()
        } catch (e: Exception) {
            AppLogger.log("Reminder", "铃声操作失败", e)
        }

        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        else
            overridePendingTransition(0, 0)
    }

    private fun handleMute() {
        val kind = ReminderKind.fromName(intent.getStringExtra(EXTRA_KIND))
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val startTime = intent.getStringExtra(EXTRA_COURSE_START_TIME).orEmpty()
        val endTime = intent.getStringExtra(EXTRA_COURSE_END_TIME).orEmpty()
        val now = System.currentTimeMillis()
        val startAtMs = parseTodayTimeMs(startTime)
        val endAtMs = parseTodayTimeMs(endTime)
        val canChangeRinger = RingerControl.canChange(this)

        val accepted =
            when {

                startAtMs != null && now < startAtMs -> {
                    MuteAlarmScheduler.scheduleMute(
                        context = this,
                        kind = kind,
                        notificationId = notificationId,
                        startAtMs = startAtMs,
                        endTime = endTime,
                    )
                    if (canChangeRinger) {
                        toast("已设置${kind.sessionLabel}静音，到点自动开启")
                    } else {
                        RingerControl.requestAccess(this)
                        toast("已设置${kind.sessionLabel}静音，但需要先允许修改勿扰状态")
                    }
                    true
                }

                endAtMs != null && now in (startAtMs ?: now)..endAtMs -> {
                    if (!canChangeRinger) {
                        RingerControl.requestAccess(this)
                        toast("需要允许修改勿扰状态才能静音")
                        false
                    } else {
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val previousMode = audioManager.ringerMode
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                        MuteAlarmScheduler.scheduleAutoUnmute(
                            context = this,
                            kind = kind,
                            notificationId = notificationId,
                            endTime = endTime,
                            restoreMode = previousMode,
                        )
                        toast("当前在${kind.sessionLabel}时段，已开启静音")
                        true
                    }
                }

                else -> {
                    toast("当前不在${kind.sessionLabel}时段，未开启静音")
                    false
                }
            }

        if (accepted && notificationId != -1) {
            getSystemService(NotificationManager::class.java)?.cancel(notificationId)
        }
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val EXTRA_KIND = "reminder_kind"
        private const val EXTRA_COURSE_START_TIME = "course_time"
        private const val EXTRA_COURSE_END_TIME = "course_end_time"
        private const val EXTRA_NOTIFICATION_ID = "notification_id"

        fun muteIntent(
            context: Context,
            kind: ReminderKind,
            notificationId: Int,
            startTime: String,
            endTime: String,
        ): Intent =
            Intent(context, MuteActionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_KIND, kind.name)
                putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_COURSE_START_TIME, startTime)
                putExtra(EXTRA_COURSE_END_TIME, endTime)
            }
    }
}
