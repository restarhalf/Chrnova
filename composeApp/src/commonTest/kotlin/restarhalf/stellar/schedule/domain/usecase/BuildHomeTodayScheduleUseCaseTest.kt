package restarhalf.stellar.schedule.domain.usecase

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuildHomeTodayScheduleUseCaseTest {

    private val useCase = BuildHomeTodayScheduleUseCase()

    /** 2026-03-02（周一）00:00 本地时区 */
    private val termStartMs = LocalDateTime.parse("2026-03-02T00:00")
        .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    private val dayMs = 24 * 60 * 60 * 1000L

    private fun course(
        name: String,
        dayOfWeek: Int = 1,
        startSection: Int = 1,
        sectionCount: Int = 2,
        weeks: List<Int> = listOf(2),
        type: Int = 0,
        remoteKey: String = "",
        originRemoteKey: String = "",
        targetWeek: Int = 0,
    ) = Course(
        name = name,
        location = "教室$name",
        teacher = "老师$name",
        dayOfWeek = dayOfWeek,
        startSection = startSection,
        sectionCount = sectionCount,
        weeks = weeks,
        color = "#FF0000",
        type = type,
        remoteKey = remoteKey,
        originRemoteKey = originRemoteKey,
        targetWeek = targetWeek,
    )

    @Test
    fun `学期中第2周周一命中周一课程`() {
        val monday = course("高等数学", dayOfWeek = 1, weeks = listOf(2))
        val tuesday = course("英语", dayOfWeek = 2, weeks = listOf(2))
        val schedule = useCase(
            courses = listOf(monday, tuesday),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs + 10 * dayMs, // 第2周
        )
        assertEquals(2, schedule.activeWeek)
        assertEquals(listOf("高等数学"), schedule.todayCourses.map { it.name })
        assertTrue(schedule.hasFirstClass)
    }

    @Test
    fun `学期未开始为假期且无今日课程`() {
        val schedule = useCase(
            courses = listOf(course("高等数学")),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs - dayMs,
        )
        assertNull(schedule.activeWeek)
        assertTrue(schedule.todayCourses.isEmpty())
        assertEquals(listOf(Period(1, 4, null)), schedule.morningItems.map { Period(it.startSection, it.endSection, it.course) })
        assertFalse(schedule.hasFirstClass)
    }

    @Test
    fun `超出总周数为假期`() {
        val schedule = useCase(
            courses = listOf(course("高等数学")),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs + 140 * dayMs, // 第21周 > 20
        )
        assertNull(schedule.activeWeek)
        assertTrue(schedule.todayCourses.isEmpty())
    }

    @Test
    fun `周次不在课程weeks内则不命中`() {
        val schedule = useCase(
            courses = listOf(course("高等数学", weeks = listOf(3))),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs + 10 * dayMs, // 第2周，课只在第3周
        )
        assertTrue(schedule.todayCourses.isEmpty())
        assertFalse(schedule.hasFirstClass)
    }

    @Test
    fun `调课覆盖隐藏原课`() {
        val original = course("高等数学", dayOfWeek = 1, startSection = 1, sectionCount = 2, remoteKey = "k1")
        val override = course(
            "高等数学", dayOfWeek = 1, startSection = 5, sectionCount = 2,
            type = 2, remoteKey = "k1#override#2#2", originRemoteKey = "k1",
            targetWeek = 2, weeks = listOf(2),
        )
        val schedule = useCase(
            courses = listOf(original, override),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs + 10 * dayMs,
        )
        // 原位置(1-2节)被调课覆盖，只剩调课后的位置(5-6节)
        assertEquals(1, schedule.todayCourses.size)
        assertEquals(5, schedule.todayCourses[0].startSection)
        assertFalse(schedule.hasFirstClass)
        assertEquals(0, schedule.morningItems.count { it.course != null })
        assertEquals(1, schedule.afternoonItems.count { it.course != null })
    }

    @Test
    fun `课程按上午下午晚上分段`() {
        val morning = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2)
        val afternoon = course("英语", dayOfWeek = 1, startSection = 5, sectionCount = 2)
        val evening = course("体育", dayOfWeek = 1, startSection = 9, sectionCount = 2)
        val schedule = useCase(
            courses = listOf(morning, afternoon, evening),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs + 10 * dayMs,
        )
        assertEquals(1, schedule.morningItems.count { it.course != null })
        assertEquals(1, schedule.afternoonItems.count { it.course != null })
        assertEquals(1, schedule.eveningItems.count { it.course != null })
    }

    @Test
    fun `空洞时段填充空闲条目`() {
        val first = course("高数", dayOfWeek = 1, startSection = 1, sectionCount = 2) // 1-2
        val fourth = course("英语", dayOfWeek = 1, startSection = 4, sectionCount = 1) // 4
        val schedule = useCase(
            courses = listOf(first, fourth),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs + 10 * dayMs,
        )
        // 期望：1-2 有课、3 空闲、4 有课
        assertEquals(
            listOf(1 to 2, 3 to 3, 4 to 4),
            schedule.morningItems.map { it.startSection to it.endSection },
        )
        assertEquals("高数", schedule.morningItems[0].course?.name)
        assertNull(schedule.morningItems[1].course)
        assertEquals("英语", schedule.morningItems[2].course?.name)
    }

    @Test
    fun `无课时段单个空闲条目覆盖整段`() {
        val schedule = useCase(
            courses = emptyList(),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs + 10 * dayMs,
        )
        assertEquals(1, schedule.morningItems.size)
        assertEquals(1, schedule.morningItems[0].startSection)
        assertEquals(4, schedule.morningItems[0].endSection)
        assertNull(schedule.morningItems[0].course)
    }

    @Test
    fun `第三节有课不算早八`() {
        val schedule = useCase(
            courses = listOf(course("高数", dayOfWeek = 1, startSection = 3, sectionCount = 2)),
            totalWeeks = 20,
            termStartMs = termStartMs,
            todayDayOfWeekMon1 = 1,
            nowMs = termStartMs + 10 * dayMs,
        )
        assertFalse(schedule.hasFirstClass)
    }

    @Test
    fun `timeRange 从时间表取节次时间`() {
        val timetable = listOf(
            TimetableSlot(1, "08:00", "08:45"),
            TimetableSlot(2, "08:55", "09:40"),
        )
        assertEquals("08:00" to "08:45", useCase.timeRange(timetable, 1, 1))
        assertEquals("08:00" to "09:40", useCase.timeRange(timetable, 1, 2))
        assertEquals("--" to "--", useCase.timeRange(timetable, 5, 6))
    }
}

/** 测试用条目投影（PeriodItem 的 course 为引用类型，比较时投影为名称更直观） */
private fun Period(startSection: Int, endSection: Int, course: Course?): Triple<Int, Int, String?> =
    Triple(startSection, endSection, course?.name)
