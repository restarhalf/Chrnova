package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.AuthProfile
import restarhalf.stellar.schedule.domain.port.AuthPort

class ObserveAuthProfileUseCase(
    private val auth: AuthPort,
) {
    operator fun invoke(): Flow<AuthProfile> = auth.observeProfile()
}
