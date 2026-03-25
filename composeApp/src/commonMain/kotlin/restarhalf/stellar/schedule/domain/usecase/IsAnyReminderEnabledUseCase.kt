package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.SettingsPort

class IsAnyReminderEnabledUseCase(
    private val settings: SettingsPort,
) {
    suspend operator fun invoke(): Boolean {
        val courseEnabled = settings.observeCourseReminderEnabled().first()
        val examEnabled = settings.observeExamReminderEnabled().first()
        return courseEnabled || examEnabled
    }
}
