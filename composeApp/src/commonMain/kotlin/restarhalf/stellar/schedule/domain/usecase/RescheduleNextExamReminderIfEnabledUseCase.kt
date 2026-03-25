package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.SettingsPort

class RescheduleNextExamReminderIfEnabledUseCase(
    private val settings: SettingsPort,
    private val fetchExaminations: FetchExaminationsUseCase,
    private val examReminder: ExamReminderPort,
) {
    suspend operator fun invoke() {
        val enabled = settings.observeExamReminderEnabled().first()
        if (!enabled) return

        val exams = fetchExaminations(semester = "", nameOrNumber = "")
        examReminder.scheduleNextReminder(exams)
    }
}
