package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

class EnsureLoggedInUseCase(
    private val authWorkflow: AuthWorkflowPort,
) {
    suspend operator fun invoke() {
        authWorkflow.ensureLoggedIn()
    }
}
