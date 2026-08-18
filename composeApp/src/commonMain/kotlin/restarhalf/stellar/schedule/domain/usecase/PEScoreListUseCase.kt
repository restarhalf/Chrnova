package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PEScoreListResponse
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.data.repository.RoomPERepository
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 体育成绩列表用例
 */
class PEScoreListUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
    private val repository: RoomPERepository? = null,
) {
    /**
     * 观察成绩列表（本地缓存）
     */
    fun observeScoreList(): Flow<List<PEYearScore>> =
        repository?.observeAllScores() ?: throw IllegalStateException("本地缓存不可用")

    /**
     * 获取成绩列表
     */
    suspend operator fun invoke(): PEScoreListResponse {
        val response = withSessionRetry(authWorkflow) { gateway.getScoreList() }
        repository?.replaceScores(response.dataArr)
        return response
    }
}
