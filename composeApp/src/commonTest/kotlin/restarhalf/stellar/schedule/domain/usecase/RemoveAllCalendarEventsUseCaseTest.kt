package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoveAllCalendarEventsUseCaseTest {

    private val calendarEvent = mock<CalendarEventPort>(MockMode.autofill)
    private val useCase = RemoveAllCalendarEventsUseCase(calendarEvent)

    @Test
    fun `课程和考试都删除时调用removeAllEvents`() = runTest {
        val result = CalendarEventPort.SyncResult.Success(10)
        everySuspend { calendarEvent.removeAllEvents() } returns result

        assertEquals(result, useCase(removeCourses = true, removeExams = true))
        verifySuspend(VerifyMode.exactly(1)) { calendarEvent.removeAllEvents() }
        verifySuspend(VerifyMode.not) { calendarEvent.removeAllCourseEvents() }
        verifySuspend(VerifyMode.not) { calendarEvent.removeAllExamEvents() }
    }

    @Test
    fun `仅删课程时调用removeAllCourseEvents`() = runTest {
        val result = CalendarEventPort.SyncResult.Success(5)
        everySuspend { calendarEvent.removeAllCourseEvents() } returns result

        assertEquals(result, useCase(removeCourses = true, removeExams = false))
        verifySuspend(VerifyMode.exactly(1)) { calendarEvent.removeAllCourseEvents() }
        verifySuspend(VerifyMode.not) { calendarEvent.removeAllEvents() }
    }

    @Test
    fun `仅删考试时调用removeAllExamEvents`() = runTest {
        val result = CalendarEventPort.SyncResult.Success(3)
        everySuspend { calendarEvent.removeAllExamEvents() } returns result

        assertEquals(result, useCase(removeCourses = false, removeExams = true))
        verifySuspend(VerifyMode.exactly(1)) { calendarEvent.removeAllExamEvents() }
        verifySuspend(VerifyMode.not) { calendarEvent.removeAllEvents() }
    }

    @Test
    fun `两个开关都关闭时返回Success0且不触碰日历`() = runTest {
        val result = useCase(removeCourses = false, removeExams = false)

        assertEquals(CalendarEventPort.SyncResult.Success(0), result)
        verifySuspend(VerifyMode.not) { calendarEvent.removeAllEvents() }
        verifySuspend(VerifyMode.not) { calendarEvent.removeAllCourseEvents() }
        verifySuspend(VerifyMode.not) { calendarEvent.removeAllExamEvents() }
    }
}
