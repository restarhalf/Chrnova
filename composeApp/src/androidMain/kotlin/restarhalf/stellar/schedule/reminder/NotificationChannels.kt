package restarhalf.stellar.schedule.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

object NotificationChannels {

    const val NEXT_DAY_CHANNEL_ID = "course_next_day"

    /** 实况/超级岛频道：课程与考试的倒计时、已开始、进行中进度共用，绕过勿扰（仅 PRIORITY 档生效）。 */
    const val LIVE_CHANNEL_ID = "course_live"

    @RequiresApi(Build.VERSION_CODES.O)
    fun ensureAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureNextDayReminder(manager)
        ensureLive(manager)
        // 旧链路残留频道：静态考试提醒卡 / 静音反馈，升级后清掉
        manager?.deleteNotificationChannel("exam_reminder_high")
        manager?.deleteNotificationChannel("course_mute_low")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun ensureLive(context: Context) {
        ensureLive(context.getSystemService(NotificationManager::class.java))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureLive(manager: NotificationManager?) {
        if (manager == null || manager.getNotificationChannel(LIVE_CHANNEL_ID) != null) return

        val channel =
            NotificationChannel(
                LIVE_CHANNEL_ID,
                "课程实况",
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .apply {
                    description = "课程与考试的倒计时、已开始与进行中进度"
                    setShowBadge(true)
                    enableLights(false)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    // 绕过勿扰：开「上课/考试勿扰」后，用户特意打开的进行中提醒仍要看得见。
                    // 完全勿扰（INTERRUPTION_FILTER_NONE）属系统级全静音，bypassDnd 无法放行。
                    setBypassDnd(true)
                }
        manager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureNextDayReminder(manager: NotificationManager?) {
        if (manager == null || manager.getNotificationChannel(NEXT_DAY_CHANNEL_ID) != null) return

        val channel =
            NotificationChannel(
                NEXT_DAY_CHANNEL_ID,
                "明日课程提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .apply {
                    description = "每晚提醒第二天的课程"
                    setShowBadge(false)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
        manager.createNotificationChannel(channel)
    }
}