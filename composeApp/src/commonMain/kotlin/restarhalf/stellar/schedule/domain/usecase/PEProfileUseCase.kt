package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEProfileResponse
import restarhalf.stellar.schedule.data.repository.PERepository
import restarhalf.stellar.schedule.domain.model.PEProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 体育学生信息用例
 */
class PEProfileUseCase(
    private val repository: PERepository,
    private val peAuth: PEAuthPort,
    private val peAuthWorkflow: PEAuthWorkflowPort,
) {
    /**
     * 获取学生信息并保存到用户档案
     */
    suspend operator fun invoke(): PEProfileResponse {
        val response = withSessionRetry(peAuthWorkflow) { repository.getProfile() }
        response.data?.let {
            peAuth.setProfile(
                PEProfile(
                    stuName = it.stuName,
                    stdNumber = it.stdNumber,
                    testCode = it.testCode,
                )
            )
        }
        return response
    }
}
