package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyncExactAlarmPermissionUseCaseTest {

    private val permission = mock<ExactAlarmPermissionPort>(MockMode.autofill)
    private val reminderSettings = mock<ReminderSettingsPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val courseReminder = mock<CourseReminderPort>(MockMode.autofill)
    private val examReminder = mock<ExamReminderPort>(MockMode.autofill)
    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val termSettings = mock<TermSettingsPort>(MockMode.autofill)

    private val rescheduleAll = RescheduleAllRemindersUseCase(
        settings = reminderSettings,
        courseRepository = courseRepository,
        timetable = timetable,
        courseReminder = courseReminder,
        examReminder = examReminder,
        collectReminderExaminations = CollectReminderExaminationsUseCase(
            fetchExaminations = FetchExaminationsUseCase(
                authWorkflow = authWorkflow,
                academic = academic,
                repository = examinationRepository,
                auth = auth,
                settings = termSettings,
            ),
            repository = examinationRepository,
        ),
        cancelAllCourseReminders = CancelAllCourseRemindersUseCase(courseReminder),
        cancelAllExamReminders = CancelAllExamRemindersUseCase(examReminder),
    )

    private val useCase = SyncExactAlarmPermissionUseCase(
        exactAlarmPermission = permission,
        isAnyReminderEnabled = IsAnyReminderEnabledUseCase(reminderSettings),
        rescheduleAllReminders = rescheduleAll,
    )

    private fun stubReminders(courseEnabled: Boolean, examEnabled: Boolean = false) {
        every { reminderSettings.observeCourseReminderEnabled() } returns flowOf(courseEnabled)
        every { reminderSettings.observeExamReminderEnabled() } returns flowOf(examEnabled)
        every { timetable.getCampus() } returns Campus.Jinshitan
        every { timetable.getTermStartMs() } returns 1000L
        every { timetable.getTotalWeeks() } returns 20
        everySuspend { courseRepository.getAllCoursesOnce() } returns emptyList()
        everySuspend { courseReminder.scheduleNextReminder(any(), any(), any(), any()) } returns
            ReminderScheduleResult.NoUpcoming
    }

    @Test
    fun `首次记录且提醒关着时只写入状态`() = runTest {
        every { permission.isExactAlarmAllowed() } returns true
        every { permission.lastKnownAllowed() } returns null
        stubReminders(courseEnabled = false)

        assertFalse(useCase())

        verify(VerifyMode.not) { courseReminder.cancelAll() }
        verifySuspend(VerifyMode.exactly(1)) { permission.setLastKnownAllowed(true) }
    }

    @Test
    fun `首次记录且提醒开着时按可能存在的模糊闹钟重排一次`() = runTest {
        every { permission.isExactAlarmAllowed() } returns true
        every { permission.lastKnownAllowed() } returns null
        stubReminders(courseEnabled = true)

        assertTrue(useCase())

        verify(VerifyMode.exactly(1)) { courseReminder.cancelAll() }
        verifySuspend(VerifyMode.exactly(1)) { permission.setLastKnownAllowed(true) }
    }

    @Test
    fun `从拒绝变为允许且提醒开着时重排`() = runTest {
        every { permission.isExactAlarmAllowed() } returns true
        every { permission.lastKnownAllowed() } returns false
        stubReminders(courseEnabled = true)

        assertTrue(useCase())

        verify(VerifyMode.exactly(1)) { courseReminder.cancelAll() }
        verifySuspend(VerifyMode.exactly(1)) {
            courseReminder.scheduleNextReminder(any(), any(), any(), any())
        }
        verifySuspend(VerifyMode.exactly(1)) { permission.setLastKnownAllowed(true) }
    }

    @Test
    fun `从拒绝变为允许但提醒都关着时不重排`() = runTest {
        every { permission.isExactAlarmAllowed() } returns true
        every { permission.lastKnownAllowed() } returns false
        stubReminders(courseEnabled = false)

        assertFalse(useCase())

        verify(VerifyMode.not) { courseReminder.cancelAll() }
        verifySuspend(VerifyMode.exactly(1)) { permission.setLastKnownAllowed(true) }
    }

    @Test
    fun `授权被收回时只更新记录`() = runTest {
        every { permission.isExactAlarmAllowed() } returns false
        every { permission.lastKnownAllowed() } returns true
        stubReminders(courseEnabled = true)

        assertFalse(useCase())

        verify(VerifyMode.not) { courseReminder.cancelAll() }
        verifySuspend(VerifyMode.exactly(1)) { permission.setLastKnownAllowed(false) }
    }

    @Test
    fun `状态没变化时不写入也不重排`() = runTest {
        every { permission.isExactAlarmAllowed() } returns true
        every { permission.lastKnownAllowed() } returns true
        stubReminders(courseEnabled = true)

        assertFalse(useCase())

        verify(VerifyMode.not) { courseReminder.cancelAll() }
        verifySuspend(VerifyMode.not) { permission.setLastKnownAllowed(any()) }
    }
}
