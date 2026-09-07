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
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncExamEventsToCalendarUseCaseTest {

    private val repository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val calendarEvent = mock<CalendarEventPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    private val useCase = SyncExamEventsToCalendarUseCase(
        observeAllExaminations = ObserveAllExaminationsUseCase(repository, auth),
        calendarEvent = calendarEvent,
        settings = settings,
    )

    private fun stubExams(vararg exams: Examination) {
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        every { repository.observeExaminationsByUserNo("2023001") } returns flowOf(exams.toList())
    }

    @Test
    fun `开关关闭时直接返回Success0且不触碰日历`() = runTest {
        every { settings.observeExamReminderEnabled() } returns flowOf(false)

        val result = useCase(selectedTerm = "2026-1")

        assertEquals(CalendarEventPort.SyncResult.Success(0), result)
        verifySuspend(VerifyMode.not) { calendarEvent.syncExamEvents(any()) }
    }

    @Test
    fun `无日历权限时返回PermissionDenied`() = runTest {
        every { settings.observeExamReminderEnabled() } returns flowOf(true)
        every { calendarEvent.hasCalendarPermission() } returns false

        val result = useCase(selectedTerm = "2026-1")

        assertEquals(CalendarEventPort.SyncResult.PermissionDenied, result)
    }

    @Test
    fun `选中学期非空时按学期过滤考试`() = runTest {
        every { settings.observeExamReminderEnabled() } returns flowOf(true)
        every { calendarEvent.hasCalendarPermission() } returns true
        stubExams(
            Examination(courseName = "高数", semesterId = "2026-1"),
            Examination(courseName = "英语", semesterId = "2025-2"),
        )
        val expected = listOf(Examination(courseName = "高数", semesterId = "2026-1"))
        everySuspend { calendarEvent.syncExamEvents(any()) } returns CalendarEventPort.SyncResult.Success(1)

        useCase(selectedTerm = "2026-1")

        verifySuspend(VerifyMode.exactly(1)) { calendarEvent.syncExamEvents(expected) }
    }

    @Test
    fun `选中学期为空时不过滤全部同步`() = runTest {
        every { settings.observeExamReminderEnabled() } returns flowOf(true)
        every { calendarEvent.hasCalendarPermission() } returns true
        val exams = listOf(
            Examination(courseName = "高数", semesterId = "2026-1"),
            Examination(courseName = "英语", semesterId = "2025-2"),
        )
        stubExams(*exams.toTypedArray())
        everySuspend { calendarEvent.syncExamEvents(any()) } returns CalendarEventPort.SyncResult.Success(2)

        useCase(selectedTerm = "")

        verifySuspend(VerifyMode.exactly(1)) { calendarEvent.syncExamEvents(exams) }
    }
}
