package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.model.PEAuthProfileResponse
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort


class PEAuthProfileUseCase(
    private val gateway: PEGateway,
    private val auth: PEAuthPort,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    
    suspend operator fun invoke(): PEAuthProfileResponse {
        val response = withSessionRetry(authWorkflow) { gateway.getProfile() }
        response.data?.let {
            auth.setProfile(
                PEAuthProfile(
                    stuName = it.stuName,
                    stdNumber = it.stdNumber,
                    testCode = it.testCode,
                )
            )
        }
        return response
    }
}
