package restarhalf.stellar.schedule.reminder.receiver

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.widget.RemoteViews
import android.widget.Toast
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
import restarhalf.stellar.schedule.domain.usecase.RescheduleNextCourseReminderIfEnabledUseCase
import restarhalf.stellar.schedule.reminder.NotificationChannels
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

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        NotificationChannels.ensureAll(context)

        val notificationId = reminderNotificationId(nowMs = System.currentTimeMillis(), time = time)

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
                startTime = time,
                endTime = endTime,
            )
        val mutePendingIntent =
            PendingIntent.getActivity(
                context,
                notificationId,
                muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val descText = buildString {
            if (time.isNotBlank()) append(time)
            if (location.isNotBlank()) {
                if (isNotEmpty()) append(" · ")
                append(location)
            }
        }

        val largeView =
            RemoteViews(context.packageName, R.layout.notify_reminder_large).apply {
                setTextViewText(R.id.notify_title, CARD_TITLE)
                setTextViewText(R.id.notify_start_time, time)
                setTextViewText(R.id.notify_end_time, endTime)
                setTextViewText(R.id.notify_detail_title, name)
                setTextViewText(R.id.notify_detail_desc, location)
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
                .setContentTitle(name)
                .setContentText(descText)
                .setCustomContentView(largeView)
                .setCustomBigContentView(largeView)
                .setCustomHeadsUpContentView(largeView)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setTimeoutAfter(REMINDER_TIMEOUT_MS)
                .addAction(muteAction)
                .build()
        notification.extras.putBoolean("miui.customHeight", true)
        notification.extras.putBoolean("miui.showAction", true)

        val nowMs = System.currentTimeMillis()
        val stage = clockStage(time = time, endTime = endTime, nowMs = nowMs)
        val timeRange = if (endTime.isBlank()) time else "$time | $endTime"
        val ticker =
            buildString {
                if (location.isNotBlank()) append(location).append("｜")
                append(if (stage.label.isBlank()) rangeOrTime(time, endTime) else "${time}上课")
            }
        val shareContent = buildString {
            appendLine(name)
            if (location.isNotBlank()) appendLine(location)
            append(rangeOrTime(time, endTime))
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
                    placeText = location,
                    ticker = ticker,
                    shareContent = shareContent,
                    muteAction = muteAction,
                    muteTitle = ISLAND_MUTE_LABEL,
                ),
            nowMs = nowMs,
        )

        notificationManager.notify(notificationId, notification)

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

    private fun reminderNotificationId(nowMs: Long, time: String): Int {
        val calendar =
            Calendar.getInstance().apply { timeInMillis = nowMs }
        val dayKey =
            calendar.get(Calendar.YEAR) * 1000 +
                calendar.get(Calendar.DAY_OF_YEAR)
        return (dayKey * 100 + (time.filter { it.isDigit() }.toIntOrNull() ?: 0)) and 0x7FFFFFFF
    }

    private fun clockStage(time: String, endTime: String, nowMs: Long): ClockStage {
        val startAt = parseTodayTimeMs(time)
        val endAt = if (endTime.isBlank()) null else parseTodayTimeMs(endTime)

        return when {
            startAt == null -> ClockStage(label = "")
            nowMs < startAt ->
                ClockStage(label = "即将上课", timerWhenMs = startAt)
            endAt != null && nowMs < endAt ->
                ClockStage(label = "距离下课", timerWhenMs = endAt)
            endAt == null ->
                ClockStage(label = "已经上课", timerWhenMs = startAt)
            else -> ClockStage(label = "已经下课")
        }
    }

    private fun rangeOrTime(time: String, endTime: String): String =
        if (endTime.isBlank()) time else "$time-${endTime}"

    private data class ClockStage(
        val label: String,
        val timerWhenMs: Long = 0L,
    ) {
        val timerType: Int get() = if (timerWhenMs > 0L) TIMER_TYPE_COUNT_DOWN_RUN else 0
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

    companion object {

        const val CHANNEL_ID = "course_reminder_high"

        private const val REMINDER_TIMEOUT_MS = 60 * 60 * 1000L
        private const val MUTE_BUTTON_LABEL = "静音"

        private const val ISLAND_MUTE_LABEL = "上课静音"

        private const val TIMER_TYPE_COUNT_DOWN_RUN = -1

        private const val CARD_TITLE = "上课提醒"

        private const val PLACE_LABEL = "地点"
    }
}
