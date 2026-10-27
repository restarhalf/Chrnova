package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort

class RescheduleNextExamReminderIfEnabledUseCase(
    private val settings: ReminderSettingsPort,
    private val collectReminderExaminations: CollectReminderExaminationsUseCase,
    private val examReminder: ExamReminderPort,
) {
    suspend operator fun invoke() {
        val enabled = settings.observeExamReminderEnabled().first()
        if (!enabled) return

        val exams = collectReminderExaminations(semester = "", nameOrNumber = "").exams
        examReminder.scheduleNextReminder(exams)
    }
}
