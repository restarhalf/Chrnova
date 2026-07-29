package restarhalf.stellar.schedule.core.text

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.time.AcademicCalendar
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * iCalendar (.ics) 导出器
 *
 * 将课程列表导出为符合 RFC 5545 的 ICS 文件,可一键导入系统日历。
 * 每门课的每个上课日展开为独立的 VEVENT,保证所有日历应用兼容。
 */
object IcsExporter {

    /**
     * 导出课程为 ICS 字符串
     *
     * @param courses 课程列表
     * @param termStartMs 学期开始时间戳(毫秒)
     * @param timetable 校区节次时间表
     * @return ICS 文件内容(UTF-8, CRLF 换行)
     */
    @OptIn(ExperimentalTime::class)
    fun export(
        courses: List<Course>,
        termStartMs: Long,
        timetable: List<TimetableSlot>,
    ): String {
        val termStartMonday = AcademicCalendar.getTermStartMonday(termStartMs)
        val slotByNum = timetable.associateBy { it.num }
        val dtstamp = formatUtcStamp(Clock.System.now())

        val sb = StringBuilder()
        sb.line("BEGIN:VCALENDAR")
        sb.line("VERSION:2.0")
        sb.line("PRODID:-//Chrnova//Schedule//CN")
        sb.line("CALSCALE:GREGORIAN")
        sb.line("METHOD:PUBLISH")

        for (course in courses) {
            val startSlot = slotByNum[course.startSection]
            val endSlot = slotByNum[course.startSection + course.sectionCount - 1]
            if (startSlot == null || endSlot == null) continue

            val startTime = ClockTime.parseToHourMinute(startSlot.start) ?: continue
            val endTime = ClockTime.parseToHourMinute(endSlot.end) ?: continue

            for (week in course.weeks) {
                if (week <= 0) continue
                val date = termStartMonday
                    .plus((week - 1) * 7 + (course.dayOfWeek - 1), DateTimeUnit.DAY)

                sb.line("BEGIN:VEVENT")
                sb.line("UID:chrnova-${course.id}-w${week}@chrnova.local")
                sb.line("DTSTAMP:$dtstamp")
                sb.line("DTSTART:${formatLocalDateTime(date, startTime.first, startTime.second)}")
                sb.line("DTEND:${formatLocalDateTime(date, endTime.first, endTime.second)}")
                sb.line("SUMMARY:${escapeText(course.name)}")
                if (course.location.isNotBlank()) {
                    sb.line("LOCATION:${escapeText(course.location)}")
                }
                val desc = buildDescription(course)
                if (desc.isNotBlank()) {
                    sb.line("DESCRIPTION:$desc")
                }
                sb.line("END:VEVENT")
            }
        }

        sb.line("END:VCALENDAR")
        return sb.toString()
    }

    private fun buildDescription(course: Course): String {
        val parts = mutableListOf<String>()
        if (course.teacher.isNotBlank()) parts.add("教师: ${course.teacher}")
        if (course.weeks.isNotEmpty()) {
            parts.add("周次: ${course.weeks.sorted().joinToString(",")}")
        }
        return escapeText(parts.joinToString(" / "))
    }

    /** ICS 文本转义:反斜杠、分号、逗号、换行 */
    private fun escapeText(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\n", "\\n")
    }

    @OptIn(ExperimentalTime::class)
    private fun formatUtcStamp(instant: Instant): String {
        val utc = instant.toLocalDateTime(TimeZone.UTC)
        return buildString {
            append(utc.year.toString().padStart(4, '0'))
            append((utc.month.ordinal + 1).toString().padStart(2, '0'))
            append(utc.day.toString().padStart(2, '0'))
            append('T')
            append(utc.hour.toString().padStart(2, '0'))
            append(utc.minute.toString().padStart(2, '0'))
            append(utc.second.toString().padStart(2, '0'))
            append('Z')
        }
    }

    private fun formatLocalDateTime(date: LocalDate, hour: Int, minute: Int): String {
        return buildString {
            append(date.year.toString().padStart(4, '0'))
            append((date.month.ordinal + 1).toString().padStart(2, '0'))
            append(date.day.toString().padStart(2, '0'))
            append('T')
            append(hour.toString().padStart(2, '0'))
            append(minute.toString().padStart(2, '0'))
            append("00")
        }
    }

    private fun StringBuilder.line(line: String) {
        append(line).append("\r\n")
    }
}
