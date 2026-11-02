package restarhalf.stellar.schedule.domain.reminder

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.domain.model.Examination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExamReminderPlannerTest {

    private val zone = TimeZone.currentSystemDefault()

    private fun msOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime(year, month, day, hour, minute).toInstant(zone).toEpochMilliseconds()

    private fun exam(
        courseName: String = "高等数学",
        time: String = "2026-03-05 09:00~11:00",
        place: String = "教三 201",
    ) = Examination(courseName = courseName, time = time, examinationPlace = place)

    @Test
    fun `解析带冒号的标准时间`() {
        assertEquals(
            LocalDateTime(2026, 3, 5, 9, 0),
            ExamReminderPlanner.parseExamStart("2026-03-05 09:00~11:00"),
        )
    }

    @Test
    fun `解析单位数小时`() {
        assertEquals(
            LocalDateTime(2026, 3, 5, 9, 0),
            ExamReminderPlanner.parseExamStart("2026-03-05 9:00 ~ 11:00"),
        )
    }

    @Test
    fun `没有波浪线的解析不了`() {
        assertNull(ExamReminderPlanner.parseExamStart("2026-03-05 09:00-11:00"))
    }

    @Test
    fun `没有任何时间的解析不了`() {
        assertNull(ExamReminderPlanner.parseExamStart("待定"))
    }

    @Test
    fun `取最近的一场未来考试`() {
        val plan =
            ExamReminderPlanner.findNext(
                exams =
                    listOf(
                        exam(courseName = "大学物理", time = "2026-03-08 14:00~16:00"),
                        exam(courseName = "高等数学", time = "2026-03-05 09:00~11:00"),
                    ),
                nowMs = msOf(2026, 3, 2, 8, 0),
            )

        assertEquals("高等数学", plan?.courseName)
        assertEquals(msOf(2026, 3, 5, 8, 44), plan?.triggerAtMs)
        assertEquals("教三 201", plan?.place)
        assertEquals("2026-03-05 09:00~11:00", plan?.timeRaw)
    }

    @Test
    fun `已经开始的考试跳过`() {
        val plan =
            ExamReminderPlanner.findNext(
                exams = listOf(exam(time = "2026-03-05 09:00~11:00")),
                nowMs = msOf(2026, 3, 5, 9, 0),
            )

        assertNull(plan)
    }

    @Test
    fun `提醒时间正好等于现在不算数`() {
        val plan =
            ExamReminderPlanner.findNext(
                exams = listOf(exam(time = "2026-03-05 09:00~11:00")),
                nowMs = msOf(2026, 3, 5, 8, 44),
            )

        assertNull(plan)
    }

    @Test
    fun `解析不了的那场跳过，其余照排`() {
        val plan =
            ExamReminderPlanner.findNext(
                exams =
                    listOf(
                        exam(courseName = "体育", time = "待定"),
                        exam(courseName = "高等数学", time = "2026-03-05 09:00~11:00"),
                    ),
                nowMs = msOf(2026, 3, 2, 8, 0),
            )

        assertEquals("高等数学", plan?.courseName)
    }

    @Test
    fun `没有考试返回 null`() {
        assertNull(ExamReminderPlanner.findNext(exams = emptyList(), nowMs = msOf(2026, 3, 2, 8, 0)))
    }
}
