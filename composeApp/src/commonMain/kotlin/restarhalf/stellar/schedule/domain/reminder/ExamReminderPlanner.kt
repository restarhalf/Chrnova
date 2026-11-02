package restarhalf.stellar.schedule.domain.reminder

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Examination

data class ExamReminderPlan(
    val triggerAtMs: Long,
    val courseName: String,
    val place: String,
    val timeRaw: String,
)

object ExamReminderPlanner {

    private val EXAM_START_REGEX = Regex("(\\d{4}-\\d{2}-\\d{2}).*?(\\d{1,2}:\\d{2})\\s*~")

    fun findNext(exams: List<Examination>, nowMs: Long): ExamReminderPlan? {
        val timeZone = TimeZone.currentSystemDefault()

        return exams
            .asSequence()
            .mapNotNull { exam ->
                val start = parseExamStart(exam.time) ?: return@mapNotNull null
                val triggerAtMs =
                    start.toInstant(timeZone).toEpochMilliseconds() -
                        ReminderTiming.LEAD_MINUTES * 60 * 1000L

                if (triggerAtMs <= nowMs) return@mapNotNull null

                ExamReminderPlan(
                    triggerAtMs = triggerAtMs,
                    courseName = exam.courseName,
                    place = exam.examinationPlace,
                    timeRaw = exam.time,
                )
            }
            .minByOrNull { it.triggerAtMs }
    }

    fun parseExamStart(raw: String): LocalDateTime? {
        val match = EXAM_START_REGEX.find(raw) ?: return null
        val dateStr = match.groupValues[1]
        val timeStr = match.groupValues[2]
        val normalized = "${dateStr}T${timeStr.padStart(5, '0')}"
        return runCatching { LocalDateTime.parse(normalized) }
            .onFailure { AppLogger.log("Reminder", "解析考试时间失败: raw=$raw", it) }
            .getOrNull()
    }
}
