package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.log.AppLogger
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
            AppLogger.log("PEAuth", "登录失败 username=$username", e)
            throw e
        }
    }
}
