package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class RescheduleRemindersUseCase(
    private val settings: ReminderSettingsPort,
    private val courseRepository: CourseRepository,
    private val timetable: TimetablePort,
    private val courseReminder: CourseReminderPort,
    private val examReminder: ExamReminderPort,
    private val collectReminderExaminations: CollectReminderExaminationsUseCase,
) {

    sealed class Result {
        data object Success : Result()
        data object CourseReminderFailed : Result()
        data object ExamReminderFailed : Result()
        data object BothRemindersFailed : Result()
    }

    suspend operator fun invoke(): Result {
        val courseReminderEnabled = settings.observeCourseReminderEnabled().first()
        val examReminderEnabled = settings.observeExamReminderEnabled().first()

        if (!courseReminderEnabled && !examReminderEnabled) {
            return Result.Success
        }

        var courseFailed = false
        var examFailed = false

        val campus = timetable.getCampus()
        val termStartMs = timetable.getTermStartMs()
        val totalWeeks = timetable.getTotalWeeks()

        if (courseReminderEnabled) {
            if (!courseReminder.hasScheduledAlarm()) {
                val courses = courseRepository.getAllCoursesOnce()
                when (
                    courseReminder.scheduleNextReminder(
                        courses = courses,
                        campus = campus,
                        termStartMs = termStartMs,
                        totalWeeks = totalWeeks
                    )) {
                    is CourseReminderPort.ScheduleResult.Scheduled -> Unit
                    is CourseReminderPort.ScheduleResult.NoUpcoming -> Unit
                    is CourseReminderPort.ScheduleResult.Failed -> courseFailed = true
                }
            }
        }

        if (examReminderEnabled) {
            if (!examReminder.hasScheduledAlarm()) {
                val exams = collectReminderExaminations(semester = "", nameOrNumber = "").exams
                when (examReminder.scheduleNextReminder(exams)) {
                    is ExamReminderPort.ScheduleResult.Scheduled -> Unit
                    is ExamReminderPort.ScheduleResult.NoUpcoming -> Unit
                    is ExamReminderPort.ScheduleResult.Failed -> examFailed = true
                }
            }
        }

        return when {
            courseFailed && examFailed -> Result.BothRemindersFailed
            courseFailed -> Result.CourseReminderFailed
            examFailed -> Result.ExamReminderFailed
            else -> Result.Success
        }
    }
}
