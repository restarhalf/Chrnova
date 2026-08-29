package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryItem
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 单科成绩历史用例
 *
 * 查询某一学年中某个体测科目的全部历史成绩记录（自动翻页聚合）。
 */
class PESubjectScoreHistoryUseCase(
    private val gateway: PEGateway,
    private val authWorkflow: PEAuthWorkflowPort,
) {
    /**
     * 获取单科成绩历史记录
     *
     * @param schoolYear 学年
     * @param subjectId 科目ID
     * @return 按测试时间倒序的历史记录列表
     */
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
