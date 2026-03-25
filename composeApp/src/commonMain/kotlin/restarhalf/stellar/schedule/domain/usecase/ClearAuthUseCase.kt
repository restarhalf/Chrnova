package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AuthPort

class ClearAuthUseCase(
    private val auth: AuthPort,
) {
    operator fun invoke() {
        auth.clear()
    }
}
