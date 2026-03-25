package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.CourseReminderPort

class CancelAllCourseRemindersUseCase(
    private val courseReminder: CourseReminderPort,
) {
    operator fun invoke() {
        courseReminder.cancelAll()
    }
}
