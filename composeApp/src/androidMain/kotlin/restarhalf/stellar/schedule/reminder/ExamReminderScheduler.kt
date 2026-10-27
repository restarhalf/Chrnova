package restarhalf.stellar.schedule.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.reminder.receiver.ExamReminderReceiver
import kotlin.time.ExperimentalTime

class ExamReminderScheduler(
    private val context: Context,
    private val settings: ObservableSettings,
) {

    data class NextExamReminder(
        val triggerAtMs: Long,
        val courseName: String,
        val place: String,
        val examTimeRaw: String
    )

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalTime::class)
    fun scheduleNextReminder(
        exams: List<Examination>,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): ScheduleResult {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val next =
            findNextReminder(exams = exams, nowMs = nowMs)
                ?: run {
                    cancelAll()
                    return ScheduleResult.NoUpcoming
                }

        val intent =
            Intent(context, ExamReminderReceiver::class.java).apply {
                putExtra(EXTRA_COURSE_NAME, next.courseName)
                putExtra(EXTRA_PLACE, next.place)
                putExtra(EXTRA_EXAM_TIME, next.examTimeRaw)
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                SINGLE_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val success =
            scheduleAlarmBestEffort(
                alarmManager = alarmManager,
                triggerAtMs = next.triggerAtMs,
                pendingIntent = pendingIntent
            )

        return if (success) {
            settings[KEY_NEXT_TRIGGER] = next.triggerAtMs
            ScheduleResult.Scheduled(next.triggerAtMs)
        } else {
            ScheduleResult.Failed
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun findNextReminder(exams: List<Examination>, nowMs: Long): NextExamReminder? {
        val now = kotlin.time.Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return exams
            .asSequence()
            .mapNotNull { exam ->
                val start = parseExamStart(exam.time) ?: return@mapNotNull null
                val trigger = kotlin.time.Instant.fromEpochMilliseconds(
                    start.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() -
                            REMINDER_MINUTES_BEFORE * 60 * 1000L
                ).toLocalDateTime(TimeZone.currentSystemDefault())

                if (trigger <= now) return@mapNotNull null

                NextExamReminder(
                    triggerAtMs = trigger.toInstant(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds(),
                    courseName = exam.courseName,
                    place = exam.examinationPlace,
                    examTimeRaw = exam.time
                )
            }
            .minByOrNull { it.triggerAtMs }
    }

    private fun parseExamStart(raw: String): LocalDateTime? {

        val regex = Regex("(\\d{4}-\\d{2}-\\d{2}).*?(\\d{1,2}:\\d{2})\\s*~")
        val match = regex.find(raw) ?: return null
        val dateStr = match.groupValues[1]
        val timeStr = match.groupValues[2]
        val normalized = "${dateStr}T${timeStr.padStart(5, '0')}"
        return runCatching {
            LocalDateTime.parse(normalized)
        }
            .onFailure {
                AppLogger.log("Reminder", "解析考试时间失败: raw=$raw", it)
            }
            .getOrNull()
    }

    fun cancelAll() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, ExamReminderReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                SINGLE_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        settings.remove(KEY_NEXT_TRIGGER)
    }

    fun getNextTriggerTime(): Long? {
        val triggerAtMs =
            if (settings.hasKey(KEY_NEXT_TRIGGER)) {
                settings.getLong(KEY_NEXT_TRIGGER, 0L)
            } else {
                return null
            }
        // 闹钟响过之后预存值会留在原地，过期就当没挂
        if (triggerAtMs <= 0L || triggerAtMs <= System.currentTimeMillis()) return null
        return triggerAtMs
    }

    companion object {
        private const val REMINDER_MINUTES_BEFORE = 15
        private const val SINGLE_REMINDER_REQUEST_CODE = 21001
        // 和课程提醒共用 reminder_codes 这个 prefs，键名必须错开
        private const val KEY_NEXT_TRIGGER = "exam_next_trigger"

        const val EXTRA_COURSE_NAME = "exam_course_name"
        const val EXTRA_PLACE = "exam_place"
        const val EXTRA_EXAM_TIME = "exam_time"
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleAlarmBestEffort(
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        pendingIntent: PendingIntent
    ): Boolean {
        return try {
            val canExact =
                alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
            true
        } catch (e: SecurityException) {
            AppLogger.log("Reminder", "设置考试提醒SecurityException", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
                true
            } catch (e2: Exception) {
                if (e2 is kotlinx.coroutines.CancellationException) throw e2
                AppLogger.log("Reminder", "设置考试提醒重试失败", e2)
                false
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AppLogger.log("Reminder", "设置考试提醒失败", e)
            false
        }
    }

    fun hasScheduledAlarm(): Boolean {
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                SINGLE_REMINDER_REQUEST_CODE,
                Intent(context, ExamReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        return pendingIntent != null
    }

    sealed class ScheduleResult {
        data class Scheduled(val triggerAtMs: Long) : ScheduleResult()

        object NoUpcoming : ScheduleResult()

        object Failed : ScheduleResult()
    }
}
