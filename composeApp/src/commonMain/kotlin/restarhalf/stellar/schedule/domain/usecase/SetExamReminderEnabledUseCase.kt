package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

class SetExamReminderEnabledUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke(enabled: Boolean) {
        settings.setExamReminderEnabled(enabled)
    }
}
