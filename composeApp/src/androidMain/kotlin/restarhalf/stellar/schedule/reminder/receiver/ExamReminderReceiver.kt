package restarhalf.stellar.schedule.reminder.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.MainActivity
import restarhalf.stellar.schedule.R
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextExamReminderIfEnabledUseCase
import restarhalf.stellar.schedule.reminder.ExamReminderScheduler

class ExamReminderReceiver : BroadcastReceiver() {

    private data class ExamPeriod(val start: String, val end: String)

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()

        val name = intent.getStringExtra(ExamReminderScheduler.EXTRA_COURSE_NAME)
        if (name.isNullOrBlank()) {
            result.finish()
            return
        }

        val place = intent.getStringExtra(ExamReminderScheduler.EXTRA_PLACE).orEmpty()
        val examTime = intent.getStringExtra(ExamReminderScheduler.EXTRA_EXAM_TIME).orEmpty()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "考试提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
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
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentIntent =
            PendingIntent.getActivity(
                context,
                name.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val notificationId = ("exam_$name$examTime").hashCode()
        val period = parseExamPeriod(examTime)

        val muteIntent =
            Intent(context, CourseReminderReceiver::class.java).apply {
                action = CourseReminderReceiver.ACTION_MUTE_DEVICE
                putExtra("notification_id", notificationId)
                putExtra("course_time", period?.start.orEmpty())
                putExtra("course_end_time", period?.end.orEmpty())
            }
        val mutePendingIntent =
            PendingIntent.getBroadcast(
                context,
                notificationId,
                muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val smallView =
            RemoteViews(context.packageName, R.layout.small_notify).apply {
                val locStr = place.ifBlank { "无地点信息" }
                setTextViewText(R.id.notification_title, "考试：$name")
                setTextViewText(R.id.notification_content, "地点:$locStr\n还有15分钟开考")
                setTextViewText(R.id.btn_mute_small, "考试静音")
                setOnClickPendingIntent(R.id.btn_mute_small, mutePendingIntent)
            }

        val largeView =
            RemoteViews(context.packageName, R.layout.large_notify).apply {
                val placeStr = if (place.isNotBlank()) "地点：$place\n" else ""
                val timeStr = if (examTime.isNotBlank()) "时间：$examTime\n" else ""
                setTextViewText(R.id.notification_title, "考试提醒：$name")
                setTextViewText(
                    R.id.notification_body,
                    "${placeStr}${timeStr}距离考试开始还有不到15分钟。"
                )
                setTextViewText(R.id.btn_mute_large, "考试静音")
                setOnClickPendingIntent(R.id.btn_mute_large, mutePendingIntent)
            }

        val notification =
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(name)
                .setContentText("距离考试还有不到15分钟")
                .setCustomContentView(smallView)
                .setCustomBigContentView(largeView)
                .setCustomHeadsUpContentView(smallView)
                .setStyle(Notification.DecoratedCustomViewStyle())
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(notificationId, notification)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val deps = Deps()
                deps.rescheduleNextReminderIfEnabled()
            } catch (e: Exception) {
                AppLogger.log("Reminder", "重新调度下次考试提醒失败", e)
            } finally {
                result.finish()
            }
        }
    }

    private class Deps : KoinComponent {
        val rescheduleNextReminderIfEnabled: RescheduleNextExamReminderIfEnabledUseCase by inject()
    }

    private fun parseExamPeriod(examTime: String): ExamPeriod? {

        val match =
            Regex("(\\d{1,2}:\\d{2})\\s*~\\s*(\\d{1,2}:\\d{2})").find(examTime) ?: return null
        return ExamPeriod(start = match.groupValues[1], end = match.groupValues[2])
    }

    companion object {
        const val CHANNEL_ID = "exam_reminder_high"
    }
}
