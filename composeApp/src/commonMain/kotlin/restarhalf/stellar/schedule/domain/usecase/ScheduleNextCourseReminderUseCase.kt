package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.CourseReminderPort

class ScheduleNextCourseReminderUseCase(
    private val getAllCoursesOnce: GetAllCoursesOnceUseCase,
    private val courseReminder: CourseReminderPort,
) {
    suspend operator fun invoke(
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ) {
        val courses = getAllCoursesOnce()
        courseReminder.scheduleNextReminder(
            courses = courses,
            campus = campus,
            termStartMs = termStartMs,
            totalWeeks = totalWeeks
        )
    }
}
