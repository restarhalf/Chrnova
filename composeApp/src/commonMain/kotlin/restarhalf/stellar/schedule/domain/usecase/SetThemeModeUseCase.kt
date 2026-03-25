package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

class SetThemeModeUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke(mode: Int) {
        settings.setThemeMode(mode)
    }
}
