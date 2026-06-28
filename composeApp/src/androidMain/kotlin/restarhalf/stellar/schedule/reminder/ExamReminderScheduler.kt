package restarhalf.stellar.schedule.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.JwxtExaminationItem
import restarhalf.stellar.schedule.reminder.receiver.ExamReminderReceiver
import kotlin.time.ExperimentalTime

class ExamReminderScheduler(private val context: Context) {

    data class NextExamReminder(
        val triggerAtMs: Long,
        val courseName: String,
        val place: String,
        val examTimeRaw: String
    )

    @OptIn(ExperimentalTime::class)
    fun scheduleNextReminder(
        exams: List<JwxtExaminationItem>,
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
            ScheduleResult.Scheduled(next.triggerAtMs)
        } else {
            ScheduleResult.Failed
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun findNextReminder(exams: List<JwxtExaminationItem>, nowMs: Long): NextExamReminder? {
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

    /** 取消已调度的考试提醒 */
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
    }

    companion object {
        private const val REMINDER_MINUTES_BEFORE = 15
        private const val SINGLE_REMINDER_REQUEST_CODE = 21001

        const val EXTRA_COURSE_NAME = "exam_course_name"
        const val EXTRA_PLACE = "exam_place"
        const val EXTRA_EXAM_TIME = "exam_time"
    }

    /**
     * 尝试调度闹钟
     *
     * @return true 表示成功设置闹钟，false 表示失败
     */
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

    /** 检查当前是否有有效的闹钟已调度 */
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

    /** 调度结果 */
    sealed class ScheduleResult {
        /** 成功调度，包含触发时间 */
        data class Scheduled(val triggerAtMs: Long) : ScheduleResult()

        /** 没有即将到来的考试 */
        object NoUpcoming : ScheduleResult()

        /** 调度失败 */
        object Failed : ScheduleResult()
    }
}
