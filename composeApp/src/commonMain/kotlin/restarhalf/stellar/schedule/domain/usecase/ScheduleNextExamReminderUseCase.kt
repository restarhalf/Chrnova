package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.ExamReminderPort

class ScheduleNextExamReminderUseCase(
    private val fetchExaminations: FetchExaminationsUseCase,
    private val examReminder: ExamReminderPort,
) {
    suspend operator fun invoke(selectedTerm: String) {
        val exams = fetchExaminations(semester = selectedTerm, nameOrNumber = "")
        examReminder.scheduleNextReminder(exams)
    }
}
