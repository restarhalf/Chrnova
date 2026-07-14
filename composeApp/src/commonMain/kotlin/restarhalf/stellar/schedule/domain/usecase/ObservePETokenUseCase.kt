package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.PEAuthPort

class ObservePETokenUseCase(
    private val peAuth: PEAuthPort,
) {
    operator fun invoke(): Flow<String?> = peAuth.observeToken()
}
