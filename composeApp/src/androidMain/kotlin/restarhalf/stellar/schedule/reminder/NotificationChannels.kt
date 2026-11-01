package restarhalf.stellar.schedule.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import restarhalf.stellar.schedule.reminder.receiver.CourseReminderReceiver
import restarhalf.stellar.schedule.reminder.receiver.ExamReminderReceiver

object NotificationChannels {

    const val MUTE_CHANNEL_ID = "course_mute_low"

    @RequiresApi(Build.VERSION_CODES.O)
    fun ensureAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureCourseReminder(manager)
        ensureExamReminder(manager)
        ensureMuteFeedback(manager)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureMuteFeedback(manager: NotificationManager) {
        if (manager.getNotificationChannel(MUTE_CHANNEL_ID) != null) return

        val channel =
            NotificationChannel(
                MUTE_CHANNEL_ID,
                "静音状态",
                NotificationManager.IMPORTANCE_LOW
            )
                .apply {
                    description = "上课静音与下课恢复铃声的结果提示"
                    setShowBadge(false)
                    setSound(null, null)
                    enableVibration(false)
                }
        manager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureCourseReminder(manager: NotificationManager) {
        if (manager.getNotificationChannel(CourseReminderReceiver.CHANNEL_ID) != null) return

        val channel =
            NotificationChannel(
                CourseReminderReceiver.CHANNEL_ID,
                "课程提醒",
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply {
                    description = "上课前推送通知提醒"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 200, 150, 200)
                    setShowBadge(true)

                    val soundUri: Uri = Settings.System.DEFAULT_NOTIFICATION_URI
                    val attrs =
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    setSound(soundUri, attrs)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
        manager.createNotificationChannel(channel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureExamReminder(manager: NotificationManager) {
        if (manager.getNotificationChannel(ExamReminderReceiver.CHANNEL_ID) != null) return

        val channel =
            NotificationChannel(
                ExamReminderReceiver.CHANNEL_ID,
                "考试提醒",
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply {
                    description = "考试前推送通知提醒"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 240, 120, 240)
                    setShowBadge(true)

                    val soundUri: Uri = Settings.System.DEFAULT_NOTIFICATION_URI
                    val attrs =
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    setSound(soundUri, attrs)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
        manager.createNotificationChannel(channel)
    }
}
