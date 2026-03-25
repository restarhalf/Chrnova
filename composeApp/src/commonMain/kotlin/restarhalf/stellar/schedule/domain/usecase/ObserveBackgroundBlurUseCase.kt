package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

class ObserveBackgroundBlurUseCase(
    private val backgroundSettings: BackgroundSettingsPort,
) {
    operator fun invoke(): Flow<Float> = backgroundSettings.observeBackgroundBlur()
}
