package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.core.error.ParseFailureException
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.model.PEScoreListResponse
import restarhalf.stellar.schedule.domain.model.PEYearScore
import restarhalf.stellar.schedule.domain.policy.distinctYearScores
import restarhalf.stellar.schedule.domain.repository.PERepository
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PEScoreListUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
    private val repository: PERepository? = null,
) {
    
    fun observeScoreList(): Flow<List<PEYearScore>> =
        repository?.observeAllScores() ?: throw IllegalStateException("本地缓存不可用")

    suspend operator fun invoke(): PEScoreListResponse {
        val response = withSessionRetry(authWorkflow) { gateway.getScoreList() }
        val raw = response.dataArr
            ?: throw ParseFailureException("体测成绩数据解析失败：缺少 data_arr 字段")
        val scores = raw.distinctYearScores()
        repository?.replaceScores(scores)
        return if (scores.size == raw.size) response else response.copy(dataArr = scores)
    }
}
