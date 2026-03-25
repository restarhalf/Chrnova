package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

class SetCourseReminderEnabledUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke(enabled: Boolean) {
        settings.setCourseReminderEnabled(enabled)
    }
}
