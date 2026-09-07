package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncCourseEventsToCalendarUseCaseTest {

    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val calendarEvent = mock<CalendarEventPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    private val useCase = SyncCourseEventsToCalendarUseCase(courseRepository, timetable, calendarEvent, settings)

    private val slots = listOf(TimetableSlot(num = 1, start = "08:00", end = "08:45"))

    private fun course(name: String) = Course(
        name = name,
        location = "教室",
        teacher = "老师",
        dayOfWeek = 1,
        startSection = 1,
        sectionCount = 2,
        weeks = listOf(1),
        color = "#FF0000",
        type = 0,
        remoteKey = "",
    )

    @Test
    fun `开关关闭时直接返回Success0且不触碰日历`() = runTest {
        every { settings.observeCourseReminderEnabled() } returns flowOf(false)

        val result = useCase(campus = Campus.Jinshitan, termStartMs = 1000L, totalWeeks = 20)

        assertEquals(CalendarEventPort.SyncResult.Success(0), result)
        verifySuspend(VerifyMode.not) { calendarEvent.syncCourseEvents(any(), any(), any()) }
    }

    @Test
    fun `无日历权限时返回PermissionDenied`() = runTest {
        every { settings.observeCourseReminderEnabled() } returns flowOf(true)
        every { calendarEvent.hasCalendarPermission() } returns false

        val result = useCase(campus = Campus.Jinshitan, termStartMs = 1000L, totalWeeks = 20)

        assertEquals(CalendarEventPort.SyncResult.PermissionDenied, result)
        verifySuspend(VerifyMode.not) { calendarEvent.syncCourseEvents(any(), any(), any()) }
    }

    @Test
    fun `开关开启且有权限时携带课程与节次表同步`() = runTest {
        every { settings.observeCourseReminderEnabled() } returns flowOf(true)
        every { calendarEvent.hasCalendarPermission() } returns true
        val courses = listOf(course("高数"))
        everySuspend { courseRepository.getAllCoursesOnce() } returns courses
        every { timetable.getCampusTimetable(any()) } returns slots
        val syncResult = CalendarEventPort.SyncResult.Success(5)
        everySuspend { calendarEvent.syncCourseEvents(any(), any(), any()) } returns syncResult

        val result = useCase(campus = Campus.Jinshitan, termStartMs = 1000L, totalWeeks = 20)

        assertEquals(syncResult, result)
        verifySuspend(VerifyMode.exactly(1)) {
            calendarEvent.syncCourseEvents(courses, 1000L, slots)
        }
    }

    @Test
    fun `同步失败结果原样返回`() = runTest {
        every { settings.observeCourseReminderEnabled() } returns flowOf(true)
        every { calendarEvent.hasCalendarPermission() } returns true
        everySuspend { courseRepository.getAllCoursesOnce() } returns emptyList()
        every { timetable.getCampusTimetable(any()) } returns slots
        val failed = CalendarEventPort.SyncResult.Failed("写入失败")
        everySuspend { calendarEvent.syncCourseEvents(any(), any(), any()) } returns failed

        val result = useCase(campus = Campus.Development, termStartMs = 1000L, totalWeeks = 20)

        assertEquals(failed, result)
    }
}
