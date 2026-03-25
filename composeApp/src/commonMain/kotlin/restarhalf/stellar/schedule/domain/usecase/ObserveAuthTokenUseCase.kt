package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.AuthPort

class ObserveAuthTokenUseCase(
    private val auth: AuthPort,
) {
    operator fun invoke(): Flow<String> = auth.observeToken()
}
