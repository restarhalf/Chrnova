package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class RefreshCourseRemindersIfEnabledUseCase(
    private val settings: SettingsPort,
    private val courseRepository: CourseRepository,
    private val courseReminder: CourseReminderPort,
) {
    suspend operator fun invoke(
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ) {
        val enabled = settings.observeCourseReminderEnabled().first()
        if (!enabled) return

        val allCourses = courseRepository.getAllCoursesOnce()
        courseReminder.scheduleNextReminder(
            courses = allCourses,
            campus = campus,
            termStartMs = termStartMs,
            totalWeeks = totalWeeks
        )
    }
}
