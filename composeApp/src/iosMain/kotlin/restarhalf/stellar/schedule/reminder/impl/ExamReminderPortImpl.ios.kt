package restarhalf.stellar.schedule.reminder.impl

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ExamReminderPortImpl(
    private val settings: ObservableSettings,
) : ExamReminderPort {

    @OptIn(ExperimentalTime::class)
    override suspend fun scheduleNextReminder(exams: List<Examination>): ExamReminderPort.ScheduleResult {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val next = findNextReminder(exams = exams, nowMs = nowMs) ?: run {
            cancelAll()
            return ExamReminderPort.ScheduleResult.NoUpcoming
        }

        val success = IosReminderNotifications.schedule(
            identifier = EXAM_REMINDER_ID,
            title = "考试提醒",
            body = "${next.courseName} ${next.place}",
            triggerAtMs = next.triggerAtMs,
        )

        return if (success) {
            settings[KEY_EXAM_SCHEDULED] = true
            settings[KEY_EXAM_TRIGGER] = next.triggerAtMs
            ExamReminderPort.ScheduleResult.Scheduled(next.triggerAtMs)
        } else {
            clearScheduledState()
            ExamReminderPort.ScheduleResult.Failed
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun hasScheduledAlarm(): Boolean {
        if (!settings.getBoolean(KEY_EXAM_SCHEDULED, false)) return false
        val triggerAtMs = settings.getLong(KEY_EXAM_TRIGGER, 0L)
        val nowMs = Clock.System.now().toEpochMilliseconds()
        if (triggerAtMs <= 0L || triggerAtMs <= nowMs) {
            cancelAll()
            return false
        }
        if (IosReminderNotifications.isPending(EXAM_REMINDER_ID) == false) {
            clearScheduledState()
            return false
        }
        return true
    }

    @OptIn(ExperimentalTime::class)
    override fun nextScheduledAtMs(): Long? {
        if (!settings.getBoolean(KEY_EXAM_SCHEDULED, false)) return null
        val triggerAtMs = settings.getLong(KEY_EXAM_TRIGGER, 0L)
        val nowMs = Clock.System.now().toEpochMilliseconds()
        // 通知响过之后预存值会留在原地，过期就当没挂
        if (triggerAtMs <= 0L || triggerAtMs <= nowMs) return null
        return triggerAtMs
    }

    override fun cancelAll() {
        IosReminderNotifications.cancel(EXAM_REMINDER_ID)
        clearScheduledState()
    }

    private fun clearScheduledState() {
        settings[KEY_EXAM_SCHEDULED] = false
        settings.remove(KEY_EXAM_TRIGGER)
    }

    @OptIn(ExperimentalTime::class)
    private fun findNextReminder(exams: List<Examination>, nowMs: Long): NextExamReminder? {
        val now = Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return exams.asSequence()
            .mapNotNull { exam ->
                val start = parseExamStart(exam.time) ?: return@mapNotNull null
                val triggerMs =
                    start.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds() -
                            REMINDER_MINUTES_BEFORE * 60 * 1000L
                val trigger = Instant.fromEpochMilliseconds(triggerMs)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                if (trigger <= now) return@mapNotNull null

                NextExamReminder(
                    triggerAtMs = triggerMs,
                    courseName = exam.courseName,
                    place = exam.examinationPlace,
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
        return runCatching { LocalDateTime.parse(normalized) }
            .onFailure {
                AppLogger.log("Reminder", "解析考试时间失败: raw=$raw", it)
            }
            .getOrNull()
    }

    private data class NextExamReminder(
        val triggerAtMs: Long,
        val courseName: String,
        val place: String,
    )

    private companion object {
        private const val REMINDER_MINUTES_BEFORE = 15
        private const val EXAM_REMINDER_ID = "exam.next.reminder"
        private const val KEY_EXAM_SCHEDULED = "ios_exam_reminder_scheduled"
        private const val KEY_EXAM_TRIGGER = "ios_exam_reminder_trigger_ms"
    }
}
