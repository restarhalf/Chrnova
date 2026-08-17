package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.remote.PEScoreListResponse
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.data.repository.PERepository
import restarhalf.stellar.schedule.data.repository.PERoomRepository
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 体育成绩列表用例
 */
class PEScoreListUseCase(
    private val repository: PERepository,
    private val peAuthWorkflow: PEAuthWorkflowPort,
    private val roomRepository: PERoomRepository? = null,
) {
    /**
     * 观察成绩列表（本地缓存）
     */
    fun observeScoreList(): Flow<List<PEYearScore>> =
        roomRepository?.observeAllScores() ?: throw IllegalStateException("本地缓存不可用")

    /**
     * 获取成绩列表
     */
    suspend operator fun invoke(): PEScoreListResponse {
        val response = withSessionRetry(peAuthWorkflow) { repository.getScoreList() }
        roomRepository?.replaceScores(response.dataArr)
        return response
    }
}
