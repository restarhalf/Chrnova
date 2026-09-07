package restarhalf.stellar.schedule.core.text

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IcsExporterTest {

    private val timetable = listOf(
        TimetableSlot(num = 1, start = "08:00", end = "08:45"),
        TimetableSlot(num = 2, start = "08:55", end = "09:40"),
    )

    /** 固定学期起点：2026-03-04T00:00:00Z（周三），学期周一 = 2026-03-02 */
    private val termStartInstant = Instant.parse("2026-03-04T00:00:00Z")

    /** 与生产同源的学期周一推导（用于独立计算期望日期） */
    private val termStartMonday: kotlinx.datetime.LocalDate = run {
        val date = termStartInstant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        date.minus(date.dayOfWeek.ordinal, DateTimeUnit.DAY)
    }

    private fun expectedDate(week: Int, dayOfWeek: Int): kotlinx.datetime.LocalDate =
        termStartMonday.plus((week - 1) * 7 + (dayOfWeek - 1), DateTimeUnit.DAY)

    private fun stamp(date: kotlinx.datetime.LocalDate, hh: Int, mm: Int): String =
        buildString {
            append(date.year.toString().padStart(4, '0'))
            append(date.month.ordinal.plus(1).toString().padStart(2, '0'))
            append(date.day.toString().padStart(2, '0'))
            append("T")
            append(hh.toString().padStart(2, '0'))
            append(mm.toString().padStart(2, '0'))
            append("00")
        }

    private fun course(
        name: String = "高等数学",
        location: String = "教学楼101",
        teacher: String = "张三",
        dayOfWeek: Int = 1,
        startSection: Int = 1,
        sectionCount: Int = 2,
        weeks: List<Int> = listOf(1),
        id: Long = 42L,
    ) = Course(
        id = id,
        name = name,
        location = location,
        teacher = teacher,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#FF5722",
    )

    private fun export(vararg courses: Course): String =
        IcsExporter.export(
            courses.toList(),
            termStartMs = termStartInstant.toEpochMilliseconds(),
            timetable = timetable,
        ).replace("\r\n", "\n")

    private fun lines(vararg courses: Course): List<String> = export(*courses).trim().split("\n")

    @Test
    fun calendarEnvelope() {
        val lines = lines(course(weeks = listOf(1)))
        assertEquals("BEGIN:VCALENDAR", lines.first())
        assertEquals("END:VCALENDAR", lines.last())
        assertEquals("VERSION:2.0", lines[1])
        assertEquals("PRODID:-//Chrnova//Schedule//CN", lines[2])
        assertTrue("CALSCALE:GREGORIAN" in lines)
        assertTrue("METHOD:PUBLISH" in lines)
    }

    @Test
    fun crlfLineEndings() {
        val raw = IcsExporter.export(
            listOf(course()),
            termStartMs = termStartInstant.toEpochMilliseconds(),
            timetable = timetable,
        )
        assertTrue(raw.contains("\r\n"))
        assertFalse(raw.contains("\n\r"))
    }

    @Test
    fun eventStartAndEndTimes() {
        val lines = lines(course(dayOfWeek = 1, startSection = 1, sectionCount = 2, weeks = listOf(1)))
        val dtStart = lines.first { it.startsWith("DTSTART:") }.removePrefix("DTSTART:")
        val dtEnd = lines.first { it.startsWith("DTEND:") }.removePrefix("DTEND:")
        assertEquals(stamp(expectedDate(1, 1), 8, 0), dtStart)
        // DTEND = 第 (startSection + sectionCount - 1) 节的 end：占第1、2节 → 第2节下课 09:40
        assertEquals(stamp(expectedDate(1, 1), 9, 40), dtEnd)
    }

    @Test
    fun weekAndDayArithmetic() {
        val lines = lines(course(dayOfWeek = 5, startSection = 2, sectionCount = 1, weeks = listOf(3)))
        val dtStart = lines.first { it.startsWith("DTSTART:") }.removePrefix("DTSTART:")
        assertEquals(stamp(expectedDate(3, 5), 8, 55), dtStart)
    }

    @Test
    fun uidEncodesCourseIdAndWeek() {
        val lines = lines(course(id = 99L, weeks = listOf(2)))
        assertEquals(
            "UID:chrnova-99-w2@chrnova.local",
            lines.first { it.startsWith("UID:") },
        )
    }

    @Test
    fun oneEventPerWeek() {
        val lines = lines(course(weeks = listOf(1, 3, 5)))
        assertEquals(3, lines.count { it == "BEGIN:VEVENT" })
        assertEquals(3, lines.count { it == "END:VEVENT" })
    }

    @Test
    fun nonPositiveWeeksSkipped() {
        val lines = lines(course(weeks = listOf(0, -1, 2)))
        assertEquals(1, lines.count { it == "BEGIN:VEVENT" })
    }

    @Test
    fun courseWithoutMatchingSlotIsSkipped() {
        val lines = lines(course(startSection = 99))
        assertFalse("BEGIN:VEVENT" in lines)
    }

    @Test
    fun unparseableSlotTimeSkipsCourse() {
        val badTimetable = listOf(TimetableSlot(num = 1, start = "八点", end = "08:45"))
        val raw = IcsExporter.export(
            listOf(course()),
            termStartMs = termStartInstant.toEpochMilliseconds(),
            timetable = badTimetable,
        )
        assertFalse(raw.contains("BEGIN:VEVENT"))
    }

    @Test
    fun icsSpecialCharactersEscaped() {
        val lines = lines(
            course(name = "数学,物理;实验\\课", location = "楼;A,101"),
        )
        assertEquals("SUMMARY:数学\\,物理\\;实验\\\\课", lines.first { it.startsWith("SUMMARY:") })
        assertEquals("LOCATION:楼\\;A\\,101", lines.first { it.startsWith("LOCATION:") })
    }

    @Test
    fun blankLocationOmitsLocationLine() {
        val lines = lines(course(location = "  "))
        assertFalse(lines.any { it.startsWith("LOCATION:") })
    }

    @Test
    fun descriptionContainsTeacherAndWeeks() {
        val lines = lines(course(teacher = "李四", weeks = listOf(5, 1, 3)))
        assertEquals("DESCRIPTION:教师: 李四 / 周次: 1\\,3\\,5", lines.first { it.startsWith("DESCRIPTION:") })
    }

    @Test
    fun blankTeacherAndNoWeeksOmitsDescription() {
        val lines = lines(course(teacher = "", weeks = emptyList()))
        assertFalse(lines.any { it.startsWith("DESCRIPTION:") })
    }
}
