package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.ExamReminderPort

class ScheduleNextExamReminderUseCase(
    private val collectReminderExaminations: CollectReminderExaminationsUseCase,
    private val examReminder: ExamReminderPort,
) {
    suspend operator fun invoke(selectedTerm: String): ExamReminderPort.ScheduleResult {
        val exams =
            collectReminderExaminations(semester = selectedTerm, nameOrNumber = "").exams
        return examReminder.scheduleNextReminder(exams)
    }
}
