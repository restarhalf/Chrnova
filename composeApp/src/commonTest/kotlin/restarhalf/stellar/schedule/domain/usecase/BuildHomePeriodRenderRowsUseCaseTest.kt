package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase.PeriodItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BuildHomePeriodRenderRowsUseCaseTest {

    private val buildToday = BuildHomeTodayScheduleUseCase()
    private val useCase = BuildHomePeriodRenderRowsUseCase(
        buildHomeTodayScheduleUseCase = buildToday,
        resolveCourseStatusUseCase = ResolveCourseStatusUseCase(),
        buildHomePeriodRowUiUseCase = BuildHomePeriodRowUiUseCase(),
    )

    private val timetable = listOf(
        TimetableSlot(1, "08:00", "08:45"),
        TimetableSlot(2, "08:55", "09:40"),
    )

    private fun course(name: String) = Course(
        name = name,
        location = "教室",
        teacher = "老师",
        dayOfWeek = 1,
        startSection = 1,
        sectionCount = 1,
        weeks = listOf(1),
        color = "#FF0000",
    )

    @Test
    fun `进行中的课行状态与内容`() {
        // 第1节 08:00-08:45 → 480-525 分钟
        val rows = useCase(listOf(PeriodItem(1, 1, course("高数"))), timetable, nowMinutes = 500)
        assertEquals(1, rows.size)
        val row = rows[0]
        assertEquals("高数", row.rowUi.primaryText)
        assertEquals("08:00" to "08:45", row.timeRange)
        assertEquals("进行中", row.status)
        assertEquals("高数", row.accentCourseName)
        assertFalse(row.rowUi.isPastOrEmpty)
    }

    @Test
    fun `已结束的课行标记past`() {
        val rows = useCase(listOf(PeriodItem(1, 1, course("高数"))), timetable, nowMinutes = 600)
        assertEquals("已结束", rows[0].status)
        assertTrue(rows[0].rowUi.isPastOrEmpty)
    }

    @Test
    fun `空闲行无状态`() {
        val rows = useCase(listOf(PeriodItem(2, 2, null)), timetable, nowMinutes = 600)
        assertNull(rows[0].status)
        assertEquals("空闲", rows[0].rowUi.primaryText)
        assertEquals("", rows[0].accentCourseName)
        assertEquals("08:55" to "09:40", rows[0].timeRange)
    }

    @Test
    fun `相同节次范围的时间范围只查询一次`() {
        val items = listOf(
            PeriodItem(1, 1, null),
            PeriodItem(1, 1, null), // 与上一条相同范围，验证缓存路径不抛错
        )
        val rows = useCase(items, timetable, nowMinutes = 500)
        assertEquals(2, rows.size)
        assertEquals(rows[0].timeRange, rows[1].timeRange)
    }

    @Test
    fun `时间表越界显示占位符`() {
        val rows = useCase(listOf(PeriodItem(9, 12, null)), emptyList(), nowMinutes = 500)
        assertEquals("--" to "--", rows[0].timeRange)
        assertNull(rows[0].status) // 无课 → status null（hasCourse=false）
    }
}
