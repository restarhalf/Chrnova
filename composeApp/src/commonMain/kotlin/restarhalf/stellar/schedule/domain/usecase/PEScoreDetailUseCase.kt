package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEDetailResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.repository.RoomPERepository
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 体育成绩详情用例
 */
class PEScoreDetailUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
    private val repository: RoomPERepository? = null,
) {
    /**
     * 观察详情数据（本地缓存）
     */
    fun observeDetailData(schoolYear: String): Flow<PEDetailData?> =
        repository?.observeDetailData(schoolYear)
            ?: throw IllegalStateException("本地缓存不可用")

    /**
     * 获取成绩详情
     */
    suspend operator fun invoke(schoolYear: String): PEDetailResponse {
        val response = withSessionRetry(authWorkflow) { gateway.getScoreDetail(schoolYear) }
        response.data?.let { repository?.saveDetailData(schoolYear, it) }
        return response
    }
}
