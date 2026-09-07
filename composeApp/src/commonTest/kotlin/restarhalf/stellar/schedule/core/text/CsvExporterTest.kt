package restarhalf.stellar.schedule.core.text

import restarhalf.stellar.schedule.domain.model.Course
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvExporterTest {

    /** appendLine 的换行符随平台不同，统一归一化为 \n 后断言 */
    private fun exportNormalized(courses: List<Course>): String =
        CsvExporter.export(courses).replace("\r\n", "\n")

    private fun course(
        name: String = "高等数学",
        teacher: String = "张三",
        location: String = "教学楼101",
        dayOfWeek: Int = 1,
        startSection: Int = 1,
        sectionCount: Int = 2,
        weeks: List<Int> = listOf(1, 2, 3),
    ) = Course(
        name = name,
        location = location,
        teacher = teacher,
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#FF5722",
    )

    @Test
    fun headerIsEmitted() {
        assertEquals("课程名称,星期,开始节数,结束节数,老师,地点,周数\n", exportNormalized(emptyList()))
    }

    @Test
    fun basicCourseRow() {
        val lines = exportNormalized(listOf(course(weeks = listOf(1, 3, 5)))).trim().split("\n")
        assertEquals(2, lines.size)
        assertEquals("高等数学,1,1,2,张三,教学楼101,1-5单", lines[1])
    }

    @Test
    fun endSectionIsStartPlusCountMinusOne() {
        val fields = exportNormalized(listOf(course(startSection = 3, sectionCount = 4)))
            .trim().split("\n")[1].split(",")
        assertEquals("3", fields[2])
        assertEquals("6", fields[3])
    }

    @Test
    fun commaInNameIsQuoted() {
        val csv = exportNormalized(listOf(course(name = "体育,篮球")))
        assertTrue(csv.contains("\"体育,篮球\""))
    }

    @Test
    fun quoteInLocationIsDoubled() {
        val csv = exportNormalized(listOf(course(location = "实验楼\"A\"")))
        assertTrue(csv.contains("\"实验楼\"\"A\"\"\""))
    }

    @Test
    fun newlineInTeacherIsQuoted() {
        val csv = exportNormalized(listOf(course(teacher = "张三\n李四")))
        assertTrue(csv.contains("\"张三\n李四\""))
    }

    @Test
    fun plainValueNotQuoted() {
        val csv = exportNormalized(listOf(course()))
        assertTrue(csv.trim().split("\n")[1].startsWith("高等数学,"))
    }

    @Test
    fun consecutiveWeeksMerged() {
        val csv = exportNormalized(listOf(course(weeks = listOf(1, 2, 3, 4))))
        assertTrue(csv.trim().endsWith("1-4"))
    }

    @Test
    fun evenWeekRunGetsDoubleSuffix() {
        val csv = exportNormalized(listOf(course(weeks = listOf(2, 4, 6))))
        assertTrue(csv.trim().endsWith("2-6双"))
    }

    @Test
    fun twoItemEvenRunStillTagged() {
        // CsvExporter 对步长2无数量门槛，两个双周也合并
        val csv = exportNormalized(listOf(course(weeks = listOf(2, 4))))
        assertTrue(csv.trim().endsWith("2-4双"))
    }

    @Test
    fun emptyWeeksRenderEmpty() {
        val fields = exportNormalized(listOf(course(weeks = emptyList())))
            .trim().split("\n")[1].split(",")
        assertEquals("", fields.last())
    }
}
