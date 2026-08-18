package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PEAuthProfileResponse
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 体育学生信息用例
 */
class PEAuthProfileUseCase(
    private val gateway: PEGateway,
    private val auth: PEAuthPort,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    /**
     * 获取学生信息并保存到用户档案
     */
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
