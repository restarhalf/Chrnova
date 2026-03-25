package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

class ObserveBackgroundImageUriUseCase(
    private val backgroundSettings: BackgroundSettingsPort,
) {
    operator fun invoke(): Flow<String?> = backgroundSettings.observeBackgroundImageUri()
}
