package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.PEDetailData
import restarhalf.stellar.schedule.domain.model.PEDetailResponse
import restarhalf.stellar.schedule.domain.policy.distinctSubjectScores
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.repository.PERepository
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PEScoreDetailUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
    private val repository: PERepository? = null,
) {
    fun observeDetailData(schoolYear: String): Flow<PEDetailData?> =
        repository?.observeDetailData(schoolYear)
            ?: throw IllegalStateException("本地缓存不可用")

    suspend operator fun invoke(schoolYear: String): PEDetailResponse {
        val response = withSessionRetry(authWorkflow) { gateway.getScoreDetail(schoolYear) }
        val detail = response.data
        if (detail == null) return response
        val subjects = detail.dataArr.distinctSubjectScores()
        val deduped = if (subjects.size == detail.dataArr.size) detail else detail.copy(dataArr = subjects)
        repository?.saveDetailData(schoolYear, deduped)
        return response.copy(data = deduped)
    }
}
