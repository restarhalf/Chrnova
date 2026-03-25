package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

class ObserveFloatingBarUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke() = settings.observeFloatingBar()
}