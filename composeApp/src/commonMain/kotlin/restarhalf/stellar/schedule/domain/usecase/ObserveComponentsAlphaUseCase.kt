package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort

class ObserveComponentsAlphaUseCase(
    private val backgroundSettings: BackgroundSettingsPort,
) {
    operator fun invoke(): Flow<Float> = backgroundSettings.observeComponentsAlpha()
}
