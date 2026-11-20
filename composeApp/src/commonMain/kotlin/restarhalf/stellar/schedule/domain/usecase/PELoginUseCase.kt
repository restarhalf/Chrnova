package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PELoginUseCase(
    private val peAuthWorkflow: PEAuthWorkflowPort,
) {
    suspend operator fun invoke(
        username: String,
        password: String,
    ) {
        try {
            peAuthWorkflow.login(
                username = username,
                password = password,
            )
        } catch (e: Exception) {
            throw e
        }
    }
}
