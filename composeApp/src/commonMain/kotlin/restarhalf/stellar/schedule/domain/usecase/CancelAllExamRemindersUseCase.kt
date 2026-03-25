package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.ExamReminderPort

class CancelAllExamRemindersUseCase(
    private val examReminder: ExamReminderPort,
) {
    operator fun invoke() {
        examReminder.cancelAll()
    }
}
