package restarhalf.stellar.schedule.reminder.receiver

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextExamReminderIfEnabledUseCase
import restarhalf.stellar.schedule.reminder.ExamReminderScheduler
import restarhalf.stellar.schedule.reminder.NotificationChannels
import restarhalf.stellar.schedule.reminder.ReminderKind
import restarhalf.stellar.schedule.reminder.ReminderStatusLabels
import restarhalf.stellar.schedule.reminder.clockStage
import restarhalf.stellar.schedule.reminder.parseTodayTimeMs

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

        NotificationChannels.ensureAll(context)

        val nowMs = System.currentTimeMillis()
        val notificationId = reminderNotificationId(name = name, examTime = examTime)
        val period = parseExamPeriod(examTime)
        val startText = period?.start.orEmpty()
        val endText = period?.end.orEmpty()

        val descText =
            buildString {
                if (period != null) append("${period.start}-${period.end}")
                if (place.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(place)
                }
            }

        val startAt = period?.let { parseTodayTimeMs(it.start) }
        val endAt = period?.let { parseTodayTimeMs(it.end) }
        val stage =
            clockStage(startAt = startAt, endAt = endAt, nowMs = nowMs, labels = STATUS_LABELS)
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

        ReminderNotifier.notify(
            context = context,
            presentation =
                ReminderPresentation(
                    kind = ReminderKind.Exam,
                    notificationId = notificationId,
                    channelId = CHANNEL_ID,
                    category = Notification.CATEGORY_REMINDER,
                    cardTitle = CARD_TITLE,
                    contentTitle = "考试：$name",
                    contentText = descText,
                    startText = startText,
                    endText = endText,
                    detailTitle = name,
                    detailText = place,
                    placeLabel = PLACE_LABEL,
                    muteLabel = ISLAND_MUTE_LABEL,
                    timeoutMs = EXAM_TIMEOUT_MS,
                    timeRange = timeRange,
                    statusLabel = stage.label,
                    timerType = stage.timerType,
                    timerWhenMs = stage.timerWhenMs,
                    ticker = ticker,
                    shareContent = shareContent,
                ),
            nowMs = nowMs,
        )

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

    private fun reminderNotificationId(name: String, examTime: String): Int =
        "exam|$name|$examTime".hashCode() and 0x7FFFFFFF

    private fun parseExamPeriod(examTime: String): ExamPeriod? {
        val match =
            Regex("(\\d{1,2}:\\d{2})\\s*~\\s*(\\d{1,2}:\\d{2})").find(examTime) ?: return null
        return ExamPeriod(start = match.groupValues[1], end = match.groupValues[2])
    }

    companion object {
        const val CHANNEL_ID = "exam_reminder_high"

        private const val EXAM_TIMEOUT_MS = 3 * 60 * 60 * 1000L

        private const val ISLAND_MUTE_LABEL = "考试静音"

        private const val CARD_TITLE = "考试提醒"

        private const val PLACE_LABEL = "考场"

        private val STATUS_LABELS =
            ReminderStatusLabels(
                beforeStart = "即将开考",
                beforeEnd = "距离结束",
                noEnd = "正在考试",
                afterEnd = "已经结束",
            )
    }
}
