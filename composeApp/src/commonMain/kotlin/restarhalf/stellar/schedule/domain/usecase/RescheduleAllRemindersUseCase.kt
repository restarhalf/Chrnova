package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class RescheduleAllRemindersUseCase(
    private val settings: ReminderSettingsPort,
    private val courseRepository: CourseRepository,
    private val timetable: TimetablePort,
    private val courseReminder: CourseReminderPort,
    private val examReminder: ExamReminderPort,
    private val collectReminderExaminations: CollectReminderExaminationsUseCase,
    private val cancelAllCourseReminders: CancelAllCourseRemindersUseCase,
    private val cancelAllExamReminders: CancelAllExamRemindersUseCase,
) {

    data class Report(
        val courseEnabled: Boolean,
        val courseResult: ReminderScheduleResult?,
        val examEnabled: Boolean,
        val examResult: ReminderScheduleResult?,
        val examFetchFailed: Boolean,
    )

    suspend operator fun invoke(): Report {
        val courseEnabled = settings.observeCourseReminderEnabled().first()
        val examEnabled = settings.observeExamReminderEnabled().first()

        cancelAllCourseReminders()
        cancelAllExamReminders()

        val courseResult = if (courseEnabled) scheduleCourse() else null

        var examResult: ReminderScheduleResult? = null
        var examFetchFailed = false
        if (examEnabled) {
            val collected = collectReminderExaminations(semester = "", nameOrNumber = "")
            if (collected.remoteFailed && collected.exams.isEmpty()) {
                examFetchFailed = true
            } else {
                examResult =
                    runCatching { examReminder.scheduleNextReminder(collected.exams) }
                        .onFailure {
                            if (it is kotlinx.coroutines.CancellationException) throw it
                        }
                        .getOrNull()
            }
        }

        return Report(
            courseEnabled = courseEnabled,
            courseResult = courseResult,
            examEnabled = examEnabled,
            examResult = examResult,
            examFetchFailed = examFetchFailed,
        )
    }

    private suspend fun scheduleCourse(): ReminderScheduleResult? =
        runCatching {
            courseReminder.scheduleNextReminder(
                courses = courseRepository.getAllCoursesOnce(),
                campus = timetable.getCampus(),
                termStartMs = timetable.getTermStartMs(),
                totalWeeks = timetable.getTotalWeeks(),
            )
        }
            .onFailure {
                if (it is kotlinx.coroutines.CancellationException) throw it
            }
            .getOrNull()
}
