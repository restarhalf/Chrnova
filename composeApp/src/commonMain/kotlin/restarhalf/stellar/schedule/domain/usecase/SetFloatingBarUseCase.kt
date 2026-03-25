package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

class SetFloatingBarUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke(mode: Int) {
        settings.setFloatingBar(mode)
    }
}