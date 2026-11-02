package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult

class ScheduleNextExamReminderUseCase(
    private val collectReminderExaminations: CollectReminderExaminationsUseCase,
    private val examReminder: ExamReminderPort,
) {
    suspend operator fun invoke(selectedTerm: String): ReminderScheduleResult {
        val exams =
            collectReminderExaminations(semester = selectedTerm, nameOrNumber = "").exams
        return examReminder.scheduleNextReminder(exams)
    }
}
