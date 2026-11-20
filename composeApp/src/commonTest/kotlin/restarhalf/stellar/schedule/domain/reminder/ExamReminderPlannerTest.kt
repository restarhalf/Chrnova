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

    @Test
    fun `planDay取当天全部考试并按开考排序`() {
        val slots =
            ExamReminderPlanner.planDay(
                exams =
                    listOf(
                        exam(courseName = "大学物理", time = "2026-03-05 14:00~16:00"),
                        exam(courseName = "高等数学", time = "2026-03-05 09:00~11:00"),
                        exam(courseName = "明天的英语", time = "2026-03-06 09:00~11:00"),
                    ),
                nowMs = msOf(2026, 3, 5, 7, 0),
                dayOffset = 0,
            )

        assertEquals(listOf("高等数学", "大学物理"), slots.map { it.name })
        assertEquals(msOf(2026, 3, 5, 9, 0), slots[0].startAtMs)
        assertEquals(msOf(2026, 3, 5, 11, 0), slots[0].endAtMs)
        assertEquals("09:00", slots[0].timeStart)
        assertEquals("11:00", slots[0].timeEnd)
    }

    @Test
    fun `planDay按偏移日取明天的考试`() {
        val slots =
            ExamReminderPlanner.planDay(
                exams =
                    listOf(
                        exam(courseName = "高等数学", time = "2026-03-05 09:00~11:00"),
                        exam(courseName = "明天的英语", time = "2026-03-06 09:00~11:00"),
                    ),
                nowMs = msOf(2026, 3, 5, 7, 0),
                dayOffset = 1,
            )

        assertEquals(listOf("明天的英语"), slots.map { it.name })
        assertEquals(msOf(2026, 3, 6, 9, 0), slots[0].startAtMs)
    }

    @Test
    fun `planDay保留考试id与考场`() {
        val slots =
            ExamReminderPlanner.planDay(
                exams =
                    listOf(
                        exam(time = "2026-03-05 09:00~11:00", place = "教三 201")
                            .copy(id = 42L),
                    ),
                nowMs = msOf(2026, 3, 5, 7, 0),
                dayOffset = 0,
            )

        assertEquals(42L, slots.single().examId)
        assertEquals("教三 201", slots.single().place)
    }

    @Test
    fun `planDay跨零点的结束时间顺延到次日`() {
        val slots =
            ExamReminderPlanner.planDay(
                exams = listOf(exam(time = "2026-03-05 23:00~01:00")),
                nowMs = msOf(2026, 3, 5, 7, 0),
                dayOffset = 0,
            )

        assertEquals(msOf(2026, 3, 5, 23, 0), slots.single().startAtMs)
        assertEquals(msOf(2026, 3, 6, 1, 0), slots.single().endAtMs)
    }

    @Test
    fun `planDay跳过待定与别的日期`() {
        val slots =
            ExamReminderPlanner.planDay(
                exams =
                    listOf(
                        exam(courseName = "体育", time = "待定"),
                        exam(courseName = "上周的考试", time = "2026-03-04 09:00~11:00"),
                        exam(courseName = "高等数学", time = "2026-03-05 09:00~11:00"),
                    ),
                nowMs = msOf(2026, 3, 5, 7, 0),
                dayOffset = 0,
            )

        assertEquals(listOf("高等数学"), slots.map { it.name })
    }

    @Test
    fun `planDay当天没有考试返回空`() {
        assertEquals(
            emptyList(),
            ExamReminderPlanner.planDay(
                exams = listOf(exam(time = "2026-03-08 09:00~11:00")),
                nowMs = msOf(2026, 3, 5, 7, 0),
                dayOffset = 0,
            ),
        )
    }
}
