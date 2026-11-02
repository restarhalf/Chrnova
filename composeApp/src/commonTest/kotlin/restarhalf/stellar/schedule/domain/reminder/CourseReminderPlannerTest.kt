package restarhalf.stellar.schedule.domain.reminder

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CourseReminderPlannerTest {

    private val zone = TimeZone.currentSystemDefault()

    private fun msOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(zone).toEpochMilliseconds()

    private val termStartMs = msOf(2026, 3, 2, 0, 0)

    private fun course(
        name: String = "高等数学",
        location: String = "教三 201",
        dayOfWeek: Int = 1,
        startSection: Int = 1,
        sectionCount: Int = 2,
        weeks: List<Int> = listOf(1),
    ) = Course(
        name = name,
        location = location,
        teacher = "张老师",
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#3366FF",
    )

    private fun find(
        courses: List<Course>,
        nowMs: Long,
        totalWeeks: Int = 20,
        startMs: Long = termStartMs,
    ) = CourseReminderPlanner.findNext(
        courses = courses,
        campus = Campus.Development,
        termStartMs = startMs,
        totalWeeks = totalWeeks,
        nowMs = nowMs,
    )

    @Test
    fun `今天还有课就排今天`() {
        val plan = find(courses = listOf(course()), nowMs = msOf(2026, 3, 2, 7, 0))

        assertEquals(msOf(2026, 3, 2, 7, 44), plan?.triggerAtMs)
        assertEquals("高等数学", plan?.courseName)
        assertEquals("教三 201", plan?.place)
        assertEquals("8:00", plan?.timeStart)
        assertEquals("9:40", plan?.timeEnd)
    }

    @Test
    fun `今天这节提醒时间已经过了就排下周同一节`() {
        val plan =
            find(
                courses = listOf(course(weeks = listOf(1, 2))),
                nowMs = msOf(2026, 3, 2, 7, 50),
            )

        assertEquals(msOf(2026, 3, 9, 7, 44), plan?.triggerAtMs)
    }

    @Test
    fun `提醒时间正好等于现在不算数`() {
        val plan = find(courses = listOf(course()), nowMs = msOf(2026, 3, 2, 7, 44))

        assertNull(plan)
    }

    @Test
    fun `没有课返回 null`() {
        assertNull(find(courses = emptyList(), nowMs = msOf(2026, 3, 2, 7, 0)))
    }

    @Test
    fun `学期还没开始一律当假期`() {
        val plan =
            find(
                courses = listOf(course()),
                nowMs = msOf(2026, 3, 2, 7, 0),
                startMs = msOf(2026, 9, 1, 0, 0),
            )

        assertNull(plan)
    }

    @Test
    fun `超出总周数也当假期`() {
        val plan =
            find(
                courses = listOf(course()),
                nowMs = msOf(2026, 3, 2, 7, 0),
                totalWeeks = 0,
            )

        assertNull(plan)
    }

    @Test
    fun `同一天两节课取更早的那节`() {
        val later = course(name = "大学物理", startSection = 3, sectionCount = 2)
        val earlier = course(name = "高等数学", startSection = 1, sectionCount = 2)

        val plan = find(courses = listOf(later, earlier), nowMs = msOf(2026, 3, 2, 6, 0))

        assertEquals("高等数学", plan?.courseName)
        assertEquals(msOf(2026, 3, 2, 7, 44), plan?.triggerAtMs)
    }

    @Test
    fun `不是今天的那一节不参与`() {
        val plan =
            find(
                courses = listOf(course(dayOfWeek = 3)),
                nowMs = msOf(2026, 3, 2, 7, 0),
            )

        assertEquals(msOf(2026, 3, 4, 7, 44), plan?.triggerAtMs)
    }
}
