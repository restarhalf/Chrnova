package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class ScheduleNextCourseReminderUseCase(
    private val courseRepository: CourseRepository,
    private val courseReminder: CourseReminderPort,
) {
    suspend operator fun invoke(
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ): CourseReminderPort.ScheduleResult {
        val courses = courseRepository.getAllCoursesOnce()
        return courseReminder.scheduleNextReminder(
            courses = courses,
            campus = campus,
            termStartMs = termStartMs,
            totalWeeks = totalWeeks,
        )
    }
}
