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
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.JwxtSessionSnapshot
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class RescheduleRemindersUseCaseTest {

    private val reminderSettings = mock<ReminderSettingsPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val timetable = mock<TimetablePort>(MockMode.autofill)
    private val courseReminder = mock<CourseReminderPort>(MockMode.autofill)
    private val examReminder = mock<ExamReminderPort>(MockMode.autofill)

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val termSettings = mock<TermSettingsPort>(MockMode.autofill)

    private val useCase = RescheduleRemindersUseCase(
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
    )

    private fun stubEnvironment(
        courseEnabled: Boolean = true,
        examEnabled: Boolean = true,
    ) {
        every { reminderSettings.observeCourseReminderEnabled() } returns flowOf(courseEnabled)
        every { reminderSettings.observeExamReminderEnabled() } returns flowOf(examEnabled)
        every { timetable.getCampus() } returns Campus.Jinshitan
        every { timetable.getTermStartMs() } returns 1000L
        every { timetable.getTotalWeeks() } returns 20
        every { auth.observeProfile() } returns flowOf(JwxtAuthProfile(userNo = "2023001"))
        every { auth.getSessionSnapshot() } returns JwxtSessionSnapshot("2023001", 0)
        every { termSettings.observeSelectedTerm() } returns flowOf("2026-1")
        everySuspend { authWorkflow.ensureLoggedIn() } returns Unit
        everySuspend { academic.fetchExaminations(any(), any()) } returns emptyList()
        everySuspend { examinationRepository.getAllExaminationsOnce() } returns emptyList()
        everySuspend { courseRepository.getAllCoursesOnce() } returns emptyList()
        everySuspend { courseReminder.scheduleNextReminder(any(), any(), any(), any()) } returns
            ReminderScheduleResult.Scheduled(5000L)
        everySuspend { examReminder.scheduleNextReminder(any()) } returns
            ReminderScheduleResult.NoUpcoming
    }

    @Test
    fun `提醒已挂时不再重复调度`() = runTest {
        stubEnvironment()
        everySuspend { courseReminder.hasScheduledAlarm() } returns true
        everySuspend { examReminder.hasScheduledAlarm() } returns true

        val result = useCase()

        assertEquals(RescheduleRemindersUseCase.Result.Success, result)
        verifySuspend(VerifyMode.not) { courseReminder.scheduleNextReminder(any(), any(), any(), any()) }
        verifySuspend(VerifyMode.not) { examReminder.scheduleNextReminder(any()) }
    }

    @Test
    fun `提醒触发后已挂标记失效时重新调度`() = runTest {
        stubEnvironment()
        everySuspend { courseReminder.hasScheduledAlarm() } returns false
        everySuspend { examReminder.hasScheduledAlarm() } returns false

        val result = useCase()

        assertEquals(RescheduleRemindersUseCase.Result.Success, result)
        verifySuspend(VerifyMode.exactly(1)) { courseReminder.scheduleNextReminder(any(), any(), any(), any()) }
        verifySuspend(VerifyMode.exactly(1)) { examReminder.scheduleNextReminder(any()) }
    }

    @Test
    fun `课程调度失败返回课程提醒失败`() = runTest {
        stubEnvironment()
        everySuspend { courseReminder.hasScheduledAlarm() } returns false
        everySuspend { examReminder.hasScheduledAlarm() } returns true
        everySuspend { courseReminder.scheduleNextReminder(any(), any(), any(), any()) } returns
            ReminderScheduleResult.Failed

        val result = useCase()

        assertEquals(RescheduleRemindersUseCase.Result.CourseReminderFailed, result)
    }

    @Test
    fun `考试调度失败返回考试提醒失败`() = runTest {
        stubEnvironment()
        everySuspend { courseReminder.hasScheduledAlarm() } returns true
        everySuspend { examReminder.hasScheduledAlarm() } returns false
        everySuspend { examReminder.scheduleNextReminder(any()) } returns
            ReminderScheduleResult.Failed

        val result = useCase()

        assertEquals(RescheduleRemindersUseCase.Result.ExamReminderFailed, result)
    }

    @Test
    fun `两项都失败返回双方失败`() = runTest {
        stubEnvironment()
        everySuspend { courseReminder.hasScheduledAlarm() } returns false
        everySuspend { examReminder.hasScheduledAlarm() } returns false
        everySuspend { courseReminder.scheduleNextReminder(any(), any(), any(), any()) } returns
            ReminderScheduleResult.Failed
        everySuspend { examReminder.scheduleNextReminder(any()) } returns
            ReminderScheduleResult.Failed

        val result = useCase()

        assertEquals(RescheduleRemindersUseCase.Result.BothRemindersFailed, result)
    }

    @Test
    fun `提醒都关闭时直接返回成功`() = runTest {
        stubEnvironment(courseEnabled = false, examEnabled = false)

        val result = useCase()

        assertEquals(RescheduleRemindersUseCase.Result.Success, result)
        verifySuspend(VerifyMode.not) { courseReminder.hasScheduledAlarm() }
        verifySuspend(VerifyMode.not) { examReminder.hasScheduledAlarm() }
    }
}
