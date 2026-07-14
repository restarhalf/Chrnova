package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

class GetBackgroundSettingsInitialUseCase(
    private val backgroundSettings: BackgroundSettingsPort,
) {
    operator fun invoke(): InitialBackgroundSettings = InitialBackgroundSettings(
        backgroundImageUri = backgroundSettings.getBackgroundImageUri(),
        backgroundAlpha = backgroundSettings.getBackgroundAlpha(),
        backgroundBlur = backgroundSettings.getBackgroundBlur(),
        componentsAlpha = backgroundSettings.getComponentsAlpha(),
    )

    data class InitialBackgroundSettings(
        val backgroundImageUri: String?,
        val backgroundAlpha: Float,
        val backgroundBlur: Float,
        val componentsAlpha: Float,
    )
}
