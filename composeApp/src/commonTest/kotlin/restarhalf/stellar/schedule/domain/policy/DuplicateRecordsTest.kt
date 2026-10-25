package restarhalf.stellar.schedule.domain.policy

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.PEAppointmentItem
import restarhalf.stellar.schedule.domain.model.PEFreeApplyItem
import restarhalf.stellar.schedule.domain.model.PESubjectHistoryItem
import restarhalf.stellar.schedule.domain.model.PESubjectScore
import restarhalf.stellar.schedule.domain.model.PEYearScore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DuplicateRecordsTest {

    @Test
    fun `考试按课程号-名称-时间-地点-座位号去重且保留首次出现`() {
        val first = Examination(id = 1, courseNumber = "C001", courseName = "高等数学", time = "2026-01-15 14:00~16:00")
        val duplicate = first.copy(id = 2, ksbz = "开卷")
        val other = first.copy(id = 3, courseNumber = "C002")

        val result = listOf(first, duplicate, other).distinctExaminations()

        assertEquals(2, result.size)
        assertSame(first, result[0])
        assertSame(other, result[1])
    }

    @Test
    fun `考试座位号不同视为同一场考试`() {
        val a = Examination(courseNumber = "C001", time = "2026-01-15 14:00~16:00", zwh = "12")
        val b = a.copy(id = 9, zwh = "12")

        assertEquals(1, listOf(a, b).distinctExaminations().size)
    }

    @Test
    fun `预约优先按 temporaryId 去重`() {
        val a = PEAppointmentItem(temporaryId = "t1", appointmentName = "体测")
        val b = PEAppointmentItem(temporaryId = "t2", appointmentName = "体测")

        assertEquals(2, listOf(a, b).distinctAppointments().size)
    }

    @Test
    fun `预约缺少 temporaryId 时按 appointmentId 去重`() {
        val a = PEAppointmentItem(appointmentId = "a1", appointmentName = "体测")
        val b = PEAppointmentItem(appointmentId = "a1", appointmentName = "体测")

        assertEquals(1, listOf(a, b).distinctAppointments().size)
    }

    @Test
    fun `预约两个 id 都缺失时退化为名称日期时段组合`() {
        val a = PEAppointmentItem(appointmentName = "体测", appointmentDate = "2026-05-01", appointmentTimes = "09:00-10:00")
        val sameSlot = a.copy(alreadyQuota = 30)
        val otherSlot = a.copy(appointmentTimes = "10:00-11:00")

        val result = listOf(a, sameSlot, otherSlot).distinctAppointments()

        assertEquals(2, result.size)
        assertSame(a, result[0])
        assertSame(otherSlot, result[1])
    }

    @Test
    fun `学年成绩按 schoolYear 去重`() {
        val scores = listOf(
            PEYearScore(schoolYear = "2024", total = 60.0),
            PEYearScore(schoolYear = "2024", total = 61.0),
            PEYearScore(schoolYear = "2025", total = 70.0),
        )

        val result = scores.distinctYearScores()

        assertEquals(listOf("2024", "2025"), result.map { it.schoolYear })
        assertEquals(60.0, result[0].total)
    }

    @Test
    fun `体测单项按 subjectId 去重`() {
        val scores = listOf(
            PESubjectScore(subjectId = "s1", subName = "肺活量"),
            PESubjectScore(subjectId = "s1", subName = "肺活量"),
            PESubjectScore(subjectId = "s2", subName = "引体向上"),
        )

        assertEquals(2, scores.distinctSubjectScores().size)
    }

    @Test
    fun `体测单项 subjectId 为空时退化为 subName`() {
        val scores = listOf(
            PESubjectScore(subjectId = "", subName = "肺活量"),
            PESubjectScore(subjectId = "", subName = "肺活量"),
        )

        assertEquals(1, scores.distinctSubjectScores().size)
    }

    @Test
    fun `成绩历史按 sourceScoreId 去重`() {
        val records = listOf(
            PESubjectHistoryItem(sourceScoreId = "r1", scoreTime = "2026-05-01"),
            PESubjectHistoryItem(sourceScoreId = "r1", scoreTime = "2026-05-01"),
            PESubjectHistoryItem(sourceScoreId = "r2", scoreTime = "2025-05-01"),
        )

        assertEquals(2, records.distinctSubjectHistory().size)
    }

    @Test
    fun `免测申请按 applyId 去重`() {
        val records = listOf(
            PEFreeApplyItem(applyId = "f1", schoolYear = "2025"),
            PEFreeApplyItem(applyId = "f1", schoolYear = "2025"),
            PEFreeApplyItem(applyId = "f2", schoolYear = "2025"),
        )

        assertEquals(2, records.distinctFreeApplies().size)
    }

    @Test
    fun `免测申请 applyId 为空时按学年-类型-时间区分`() {
        val records = listOf(
            PEFreeApplyItem(applyId = "", schoolYear = "2025", freeApplyType = "1", applyTime = "2026-01-01"),
            PEFreeApplyItem(applyId = "", schoolYear = "2025", freeApplyType = "1", applyTime = "2026-01-01"),
            PEFreeApplyItem(applyId = "", schoolYear = "2025", freeApplyType = "2", applyTime = "2026-01-02"),
        )

        assertEquals(2, records.distinctFreeApplies().size)
    }
}
