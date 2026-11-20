package restarhalf.stellar.schedule.domain.reminder

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.domain.model.Examination
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class ExamReminderPlan(
    val triggerAtMs: Long,
    val courseName: String,
    val place: String,
    val timeRaw: String,
)

/** 某一场考试的绝对起止时刻，调度闹钟与考试勿扰共用。 */
data class ExamSlot(
    val examId: Long,
    val name: String,
    val place: String,
    val startAtMs: Long,
    val endAtMs: Long,
    val timeStart: String,
    val timeEnd: String,
)

object ExamReminderPlanner {

    private val EXAM_START_REGEX = Regex("(\\d{4}-\\d{2}-\\d{2}).*?(\\d{1,2}:\\d{2})\\s*~")

    private val EXAM_PERIOD_REGEX =
        Regex("(\\d{4}-\\d{2}-\\d{2}).*?(\\d{1,2}:\\d{2})\\s*~\\s*(\\d{1,2}:\\d{2})")

    /**
     * 指定偏移日（0=今天）的全部考试，按开考时刻排序，骨架照 [CourseReminderPlanner.planDay]。
     *
     * 时间待定 / 解析失败的考试跳过；结束时间早于开始（跨零点）时顺延到次日。
     */
    @OptIn(ExperimentalTime::class)
    fun planDay(
        exams: List<Examination>,
        nowMs: Long,
        dayOffset: Int,
    ): List<ExamSlot> {
        val timeZone = TimeZone.currentSystemDefault()
        val targetDay =
            Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date
                .plus(dayOffset, DateTimeUnit.DAY)

        return exams
            .mapNotNull { exam ->
                val match = EXAM_PERIOD_REGEX.find(exam.time) ?: return@mapNotNull null
                val date =
                    runCatching { kotlinx.datetime.LocalDate.parse(match.groupValues[1]) }
                        .getOrNull() ?: return@mapNotNull null
                if (date != targetDay) return@mapNotNull null

                val start = parseClock(match.groupValues[2]) ?: return@mapNotNull null
                val end = parseClock(match.groupValues[3]) ?: return@mapNotNull null
                val dayStartMs = targetDay.atStartOfDayIn(timeZone).toEpochMilliseconds()
                val startAtMs = dayStartMs + start * 60_000L
                val rawEndAtMs = dayStartMs + end * 60_000L
                val endAtMs = if (end <= start) rawEndAtMs + 24 * 60 * 60 * 1000L else rawEndAtMs

                ExamSlot(
                    examId = exam.id,
                    name = exam.courseName,
                    place = exam.examinationPlace,
                    startAtMs = startAtMs,
                    endAtMs = endAtMs,
                    timeStart = match.groupValues[2],
                    timeEnd = match.groupValues[3],
                )
            }
            .sortedBy { it.startAtMs }
    }

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
        return runCatching { LocalDateTime.parse(normalized) }.getOrNull()
    }

    /** "9:00" / "09:00" → 当天 0 点起的分钟数。 */
    private fun parseClock(raw: String): Int? {
        val parts = raw.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }
}