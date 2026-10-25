package restarhalf.stellar.schedule.reminder.receiver

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
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
import restarhalf.stellar.schedule.reminder.NotificationChannels

class ExamReminderReceiver : BroadcastReceiver() {

    private data class ExamPeriod(val start: String, val end: String)

    @RequiresApi(Build.VERSION_CODES.O)
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

        // 频道统一由 NotificationChannels 建
        NotificationChannels.ensureAll(context)

        val notificationId = ("exam_$name$examTime").hashCode()
        val period = parseExamPeriod(examTime)

        val launchIntent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        val contentIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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

        val title = "考试：$name"
        val descText = buildString {
            if (period != null) append("${period.start}-${period.end}")
            if (place.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(place)
            }
        }

        val smallView =
            RemoteViews(context.packageName, R.layout.notify_reminder_small).apply {
                setTextViewText(R.id.notify_title, title)
                setTextViewText(R.id.notify_desc, descText)
                setTextViewText(R.id.notify_button_text, MUTE_BUTTON_LABEL)
                setOnClickPendingIntent(R.id.notify_button_layout, mutePendingIntent)
                setOnClickPendingIntent(R.id.notify_root, contentIntent)
            }

        val largeView =
            RemoteViews(context.packageName, R.layout.notify_reminder_large).apply {
                setTextViewText(R.id.notify_title, CARD_TITLE)
                setTextViewText(R.id.notify_start_time, period?.start.orEmpty())
                setTextViewText(R.id.notify_end_time, period?.end.orEmpty())
                setTextViewText(R.id.notify_detail_title, name)
                setTextViewText(R.id.notify_detail_desc, place)
                setTextViewText(R.id.notify_button_text, MUTE_BUTTON_LABEL)
                setOnClickPendingIntent(R.id.notify_button_layout, mutePendingIntent)
                setOnClickPendingIntent(R.id.notify_root, contentIntent)
            }

        // 与小爱一致：不用 DecoratedCustomViewStyle，避免系统再叠一层自带标题栏
        val notification =
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(descText)
                .setCustomContentView(smallView)
                .setCustomBigContentView(largeView)
                .setCustomHeadsUpContentView(smallView)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(EXAM_TIMEOUT_MS)
                .build()
        notification.extras.putBoolean("miui.customHeight", true)
        notification.extras.putBoolean("miui.showAction", true)

        notificationManager.notify(notificationId, notification)

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val deps = Deps()
                deps.rescheduleNextReminderIfEnabled()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
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

        private const val EXAM_TIMEOUT_MS = 3 * 60 * 60 * 1000L
        private const val MUTE_BUTTON_LABEL = "静音"

        /** 展开态大卡的标题（小爱是服务端下发的卡片标题，这里用固定文案代替） */
        private const val CARD_TITLE = "考试提醒"
    }
}
