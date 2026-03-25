package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

class SetBackgroundImageUriUseCase(
    private val backgroundSettings: BackgroundSettingsPort,
) {
    operator fun invoke(uri: String?) {
        backgroundSettings.setBackgroundImageUri(uri)
    }
}
