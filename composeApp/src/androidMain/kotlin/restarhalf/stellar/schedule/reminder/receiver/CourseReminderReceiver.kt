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
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextCourseReminderIfEnabledUseCase
import restarhalf.stellar.schedule.reminder.NotificationChannels
import restarhalf.stellar.schedule.reminder.ReminderKind
import restarhalf.stellar.schedule.reminder.ReminderStatusLabels
import restarhalf.stellar.schedule.reminder.clockStage
import restarhalf.stellar.schedule.reminder.parseTodayTimeMs
import java.util.Calendar

class CourseReminderReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()

        val name = intent.getStringExtra("course_name")
        if (name.isNullOrBlank()) {
            result.finish()
            return
        }
        val location = intent.getStringExtra("course_location") ?: ""
        val time = intent.getStringExtra("course_time") ?: ""
        val endTime = intent.getStringExtra("course_end_time") ?: ""

        NotificationChannels.ensureAll(context)

        val nowMs = System.currentTimeMillis()
        val notificationId = reminderNotificationId(nowMs = nowMs, name = name, time = time)

        val descText =
            buildString {
                if (time.isNotBlank()) append(time)
                if (location.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(location)
                }
            }

        val startAt = parseTodayTimeMs(time)
        val endAt = if (endTime.isBlank()) null else parseTodayTimeMs(endTime)
        val stage =
            clockStage(startAt = startAt, endAt = endAt, nowMs = nowMs, labels = STATUS_LABELS)
        val timeRange = if (endTime.isBlank()) time else "$time | $endTime"
        val ticker =
            buildString {
                if (location.isNotBlank()) append(location).append("｜")
                append(if (stage.label.isBlank()) rangeOrTime(time, endTime) else "${time}上课")
            }
        val shareContent =
            buildString {
                appendLine(name)
                if (location.isNotBlank()) appendLine(location)
                append(rangeOrTime(time, endTime))
            }

        ReminderNotifier.notify(
            context = context,
            presentation =
                ReminderPresentation(
                    kind = ReminderKind.Course,
                    notificationId = notificationId,
                    channelId = CHANNEL_ID,
                    category = Notification.CATEGORY_ALARM,
                    cardTitle = CARD_TITLE,
                    contentTitle = name,
                    contentText = descText,
                    startText = time,
                    endText = endTime,
                    detailTitle = name,
                    detailText = location,
                    placeLabel = PLACE_LABEL,
                    muteLabel = ISLAND_MUTE_LABEL,
                    timeoutMs = reminderTimeoutMs(startAt),
                    timeRange = timeRange,
                    statusLabel = stage.label,
                    timerType = stage.timerType,
                    timerWhenMs = stage.timerWhenMs,
                    ticker = ticker,
                    shareContent = shareContent,
                ),
            nowMs = nowMs,
        )

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val deps = Deps()
                deps.rescheduleNextReminderIfEnabled()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("Reminder", "重新调度下次提醒失败", e)
            } finally {
                result.finish()
            }
        }
    }

    private class Deps : KoinComponent {
        val rescheduleNextReminderIfEnabled: RescheduleNextCourseReminderIfEnabledUseCase by inject()
    }

    private fun reminderNotificationId(nowMs: Long, name: String, time: String): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMs }
        return "$name|$time|${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.DAY_OF_YEAR)}"
            .hashCode() and 0x7FFFFFFF
    }

    private fun rangeOrTime(time: String, endTime: String): String =
        if (endTime.isBlank()) time else "$time-$endTime"

    private fun reminderTimeoutMs(startAt: Long?): Long {
        if (startAt == null) return REMINDER_TIMEOUT_FALLBACK_MS
        return (startAt + TIMEOUT_AFTER_CLASS_START_MS - System.currentTimeMillis())
            .coerceAtLeast(MIN_TIMEOUT_MS)
    }

    companion object {

        const val CHANNEL_ID = "course_reminder_high"

        private const val TIMEOUT_AFTER_CLASS_START_MS = 10 * 60 * 1000L
        private const val REMINDER_TIMEOUT_FALLBACK_MS = 60 * 60 * 1000L
        private const val MIN_TIMEOUT_MS = 5 * 60 * 1000L

        private const val ISLAND_MUTE_LABEL = "上课静音"

        private const val CARD_TITLE = "上课提醒"

        private const val PLACE_LABEL = "地点"

        private val STATUS_LABELS =
            ReminderStatusLabels(
                beforeStart = "即将上课",
                beforeEnd = "距离下课",
                noEnd = "已经上课",
                afterEnd = "已经下课",
            )
    }
}
