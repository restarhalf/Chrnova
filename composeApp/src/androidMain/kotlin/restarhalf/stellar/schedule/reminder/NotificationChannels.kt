package restarhalf.stellar.schedule.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import restarhalf.stellar.schedule.reminder.receiver.CourseReminderReceiver
import restarhalf.stellar.schedule.reminder.receiver.ExamReminderReceiver

object NotificationChannels {

    fun ensureAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        ensureCourseReminder(manager)
        ensureExamReminder(manager)
    }

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
