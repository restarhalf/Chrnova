package restarhalf.stellar.schedule.reminder.receiver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.widget.Toast
import restarhalf.stellar.schedule.core.log.AppLogger

/**
 * 改系统铃声的中转 Activity。
 *
 * 为什么不用 BroadcastReceiver：MIUI/HyperOS 的 AudioHardening 只允许前台进程改铃声，
 * 后台调 AudioManager.setRingerMode 会被直接忽略（dumpsys audio 里留下
 * "AudioHardening volume control ... ignored, level: partial"）。
 * 通知点击和闹钟到点都可能在后台，所以统一走这个透明 Activity —— 它一被拉起就在前台。
 *
 * 动作放在 onResume 而不是 onCreate：onCreate 阶段进程还没被提到前台，
 * 实测仍会被加固策略按后台处理；到 onResume 时 Activity 已经在屏幕上，才算真前台。
 *
 * 这是纯副作用页面：不渲染任何内容，处理完立刻 finish。
 */
class MuteActionActivity : Activity() {

    private var handled = false

    override fun onResume() {
        super.onResume()
        // onResume 可能因为从后台切回来再次触发，动作只做一次
        if (handled) return
        handled = true

        try {
            when (intent.getStringExtra(EXTRA_MODE)) {
                MODE_UNMUTE -> restoreRinger()
                else -> handleMute()
            }
        } catch (e: Exception) {
            AppLogger.log("Reminder", "铃声操作失败", e)
        }

        finish()
        overridePendingTransition(0, 0)
    }

    /** 到点恢复铃声。 */
    private fun restoreRinger() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val restoreMode =
            intent.getIntExtra(EXTRA_RESTORE_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)
        audioManager.ringerMode = restoreMode
        toast("课程已结束，已恢复铃声")
    }

    /** 点静音按钮：还没上课就排一个到点闹钟，正在上课就立刻静音并排恢复闹钟。 */
    private fun handleMute() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val previousMode = audioManager.ringerMode
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val startTime = intent.getStringExtra(EXTRA_COURSE_START_TIME).orEmpty()
        val endTime = intent.getStringExtra(EXTRA_COURSE_END_TIME).orEmpty()
        val now = System.currentTimeMillis()
        val startAtMs = MuteAlarmScheduler.parseTodayTimeMs(startTime)
        val endAtMs = MuteAlarmScheduler.parseTodayTimeMs(endTime)

        when {
            // 闹钟在开课时触发时，now 必然已经 >= startAt，落到下面的分支
            startAtMs != null && now < startAtMs -> {
                MuteAlarmScheduler.scheduleClassMute(
                    context = this,
                    notificationId = notificationId,
                    startAtMs = startAtMs,
                    startTime = startTime,
                    endTime = endTime,
                )
                toast("已设置上课静音，到点自动开启")
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
            }

            else -> toast("当前不在上课时段，未开启静音")
        }
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

        /** 通知里静音按钮的点击目标。 */
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

        /** 到点自动静音的闹钟目标。 */
        fun applyMuteIntent(
            context: Context,
            notificationId: Int,
            startTime: String,
            endTime: String,
        ): Intent =
            muteIntent(context, notificationId, startTime, endTime)

        /** 下课恢复铃声的闹钟目标。 */
        fun unmuteIntent(context: Context, restoreMode: Int): Intent =
            Intent(context, MuteActionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_MODE, MODE_UNMUTE)
                putExtra(EXTRA_RESTORE_RINGER_MODE, restoreMode)
            }
    }
}
