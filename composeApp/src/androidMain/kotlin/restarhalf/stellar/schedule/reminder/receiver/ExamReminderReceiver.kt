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
import java.util.Calendar

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
            MuteActionActivity.muteIntent(
                context = context,
                notificationId = notificationId,
                startTime = period?.start.orEmpty(),
                endTime = period?.end.orEmpty(),
            )
        val mutePendingIntent =
            PendingIntent.getActivity(
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

        val muteAction =
            Notification.Action.Builder(
                null,
                ISLAND_MUTE_LABEL,
                mutePendingIntent,
            ).build()

        val notification =
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(descText)
                .setCustomContentView(largeView)
                .setCustomBigContentView(largeView)
                .setCustomHeadsUpContentView(largeView)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(EXAM_TIMEOUT_MS)
                .addAction(muteAction)
                .build()
        notification.extras.putBoolean("miui.customHeight", true)
        notification.extras.putBoolean("miui.showAction", true)

        val nowMs = System.currentTimeMillis()
        val stage = clockStage(period = period, nowMs = nowMs)
        val timeRange = if (period == null) examTime else "${period.start}-${period.end}"
        val ticker =
            buildString {
                if (place.isNotBlank()) append(place).append("｜")
                append(if (stage.label.isBlank()) timeRange else stage.label)
            }
        val shareContent =
            buildString {
                appendLine(name)
                if (place.isNotBlank()) appendLine(place)
                append(timeRange)
            }

        FocusNotification.attach(
            context = context,
            notification = notification,
            content =
                FocusContent(
                    courseName = name,
                    timeRange = timeRange,
                    statusLabel = stage.label,
                    timerType = stage.timerType,
                    timerWhenMs = stage.timerWhenMs,
                    placeLabel = PLACE_LABEL,
                    placeText = place,
                    ticker = ticker,
                    shareContent = shareContent,
                    muteAction = muteAction,
                    muteTitle = ISLAND_MUTE_LABEL,
                ),
            nowMs = nowMs,
        )

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

    private fun clockStage(period: ExamPeriod?, nowMs: Long): ClockStage {
        if (period == null) return ClockStage(label = "")

        val startAt = parseTodayTimeMs(period.start)
        val endAt = parseTodayTimeMs(period.end)
        return when {
            startAt == null -> ClockStage(label = "")
            nowMs < startAt -> ClockStage(label = "即将开考", timerWhenMs = startAt)
            endAt != null && nowMs < endAt -> ClockStage(label = "距离结束", timerWhenMs = endAt)
            endAt == null -> ClockStage(label = "正在考试", timerWhenMs = startAt)
            else -> ClockStage(label = "已经结束")
        }
    }

    private fun parseTodayTimeMs(time: String): Long? {
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

    private data class ClockStage(
        val label: String,
        val timerWhenMs: Long = 0L,
    ) {
        val timerType: Int get() = if (timerWhenMs > 0L) TIMER_TYPE_COUNT_DOWN_RUN else 0
    }

    companion object {
        const val CHANNEL_ID = "exam_reminder_high"

        private const val EXAM_TIMEOUT_MS = 3 * 60 * 60 * 1000L
        private const val MUTE_BUTTON_LABEL = "静音"

        private const val ISLAND_MUTE_LABEL = "考试静音"

        private const val TIMER_TYPE_COUNT_DOWN_RUN = -1

        private const val CARD_TITLE = "考试提醒"

        private const val PLACE_LABEL = "考场"
    }
}
