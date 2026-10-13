package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryItem
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

class PESubjectScoreHistoryUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    suspend operator fun invoke(schoolYear: String, subjectId: String): List<PESubjectHistoryItem> {
        val items = mutableListOf<PESubjectHistoryItem>()
        var pageNum = 1
        while (pageNum <= MAX_PAGES) {
            val response = withSessionRetry(authWorkflow) {
                gateway.getSubjectScoreHistory(
                    schoolYear = schoolYear,
                    subjectId = subjectId,
                    pageNum = pageNum,
                    pageSize = PAGE_SIZE,
                )
            }
            val data = response.data ?: break
            items += data.dataList
            if (data.dataList.isEmpty() || items.size >= data.totalRows) break
            pageNum++
        }
        return items.sortedByDescending { it.scoreTime }
    }

    private companion object {
        const val PAGE_SIZE = 50
        const val MAX_PAGES = 10
    }
}
