package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

class LogoutUseCase(
    private val authWorkflow: AuthWorkflowPort,
) {
    operator fun invoke() {
        authWorkflow.logout()
    }
}
