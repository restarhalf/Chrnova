package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

class SetShowNonCurrentWeekUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke(show: Boolean) {
        settings.setShowNonCurrentWeek(show)
    }
}
