package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort

class RescheduleNextCourseReminderIfEnabledUseCase(
    private val settings: SettingsPort,
    private val timetable: TimetablePort,
    private val getAllCoursesOnce: GetAllCoursesOnceUseCase,
    private val courseReminder: CourseReminderPort,
) {
    suspend operator fun invoke() {
        val enabled = settings.observeCourseReminderEnabled().first()
        if (!enabled) return

        val campus = timetable.getCampus()
        val termStartMs = timetable.getTermStartMs()
        val totalWeeks = timetable.getTotalWeeks()

        val courses = getAllCoursesOnce()
        courseReminder.scheduleNextReminder(
            courses = courses,
            campus = campus,
            termStartMs = termStartMs,
            totalWeeks = totalWeeks
        )
    }
}
