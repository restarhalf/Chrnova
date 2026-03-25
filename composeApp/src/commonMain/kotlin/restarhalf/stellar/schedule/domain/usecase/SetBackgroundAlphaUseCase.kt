package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

class SetBackgroundAlphaUseCase(
    private val backgroundSettings: BackgroundSettingsPort,
) {
    operator fun invoke(value: Float) {
        backgroundSettings.setBackgroundAlpha(value)
    }
}
