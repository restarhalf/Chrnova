package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.TimetablePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuildScheduleUiStateUseCaseTest {

    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val useCase = BuildScheduleUiStateUseCase(timetable)

    /** 2026-03-02（周一）00:00 本地时区 */
    private val termStartMs = LocalDateTime.parse("2026-03-02T00:00")
        .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    private val dayMs = 24 * 60 * 60 * 1000L

    private fun stubTimetable() {
        every { timetable.getCampusTimetable(any()) } returns listOf(
            TimetableSlot(1, "08:00", "08:45"),
            TimetableSlot(2, "08:55", "09:40"),
        )
    }

    @Test
    fun `学期中返回非假期与对应周次页码`() {
        stubTimetable()
        val state = useCase(
            campus = Campus.Jinshitan,
            totalWeeks = 20,
            termStartMs = termStartMs,
            nowMs = termStartMs + 10 * dayMs, // 第2周
        )
        assertFalse(state.detectedWeekInfo.isHoliday)
        assertEquals(2, state.detectedWeekInfo.week)
        assertFalse(state.includeWeek0)
        assertEquals(1, state.pagerInitialPage) // week-1
        assertEquals(20, state.pagerPageCount)
        assertEquals(2, state.timetable.size)
        verify(VerifyMode.exactly(1)) { timetable.getCampusTimetable(any()) }
    }

    @Test
    fun `学期开始前为假期且包含第0周`() {
        stubTimetable()
        val state = useCase(
            campus = Campus.Development,
            totalWeeks = 20,
            termStartMs = termStartMs,
            nowMs = termStartMs - dayMs,
        )
        assertTrue(state.detectedWeekInfo.isHoliday)
        assertTrue(state.includeWeek0)
        assertEquals(0, state.pagerInitialPage)
        assertEquals(21, state.pagerPageCount) // totalWeeks + 1
    }

    @Test
    fun `超出总周数为假期`() {
        stubTimetable()
        val state = useCase(
            campus = Campus.Jinshitan,
            totalWeeks = 20,
            termStartMs = termStartMs,
            nowMs = termStartMs + 140 * dayMs,
        )
        assertTrue(state.detectedWeekInfo.isHoliday)
        assertEquals(21, state.pagerPageCount)
    }

    @Test
    fun `pageToWeek与weekToPage往返一致`() {
        // 含第0周
        assertEquals(0, useCase.pageToWeek(0, includeWeek0 = true))
        assertEquals(5, useCase.pageToWeek(5, includeWeek0 = true))
        assertEquals(0, useCase.weekToPage(0, includeWeek0 = true))
        // 不含第0周
        assertEquals(1, useCase.pageToWeek(0, includeWeek0 = false))
        assertEquals(0, useCase.weekToPage(1, includeWeek0 = false))
        assertEquals(7, useCase.weekToPage(8, includeWeek0 = false))
    }

    @Test
    fun `detectWeekInfo 假期周返回0`() {
        val info = useCase.detectWeekInfo(
            totalWeeks = 20,
            termStartMs = termStartMs,
            nowMs = termStartMs - dayMs,
        )
        assertTrue(info.isHoliday)
        assertEquals(0, info.week)
        assertEquals(-1, info.diffDays)
    }
}
