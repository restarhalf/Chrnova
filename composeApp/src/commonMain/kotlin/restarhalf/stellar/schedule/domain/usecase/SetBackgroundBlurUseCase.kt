package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

class SetBackgroundBlurUseCase(
    private val backgroundSettings: BackgroundSettingsPort,
) {
    operator fun invoke(value: Float) {
        backgroundSettings.setBackgroundBlur(value)
    }
}
