package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryData
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryItem
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryResponse
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import kotlin.test.Test
import kotlin.test.assertEquals

class PESubjectScoreHistoryUseCaseTest {

    private val gateway = mock<PEGateway>(MockMode.autofill)
    private val authWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)
    private val useCase = PESubjectScoreHistoryUseCase(gateway, authWorkflow)

    private fun item(time: String) = PESubjectHistoryItem(scoreTime = time)

    @Test
    fun `单页数据直接返回并按测试时间倒序`() = runTest {
        everySuspend { gateway.getSubjectScoreHistory(any(), any(), any(), any()) } returns
            PESubjectHistoryResponse(
                data = PESubjectHistoryData(
                    dataList = listOf(item("2025-10-01"), item("2025-11-01")),
                    totalRows = 2,
                )
            )

        val result = useCase(schoolYear = "2025-2026", subjectId = "S1")

        assertEquals(listOf("2025-11-01", "2025-10-01"), result.map { it.scoreTime })
    }

    @Test
    fun `多页数据自动翻页聚合`() = runTest {
        var pageRequests = 0
        everySuspend { gateway.getSubjectScoreHistory(any(), any(), any(), any()) } calls {
            (_: String, _: String, page: Int, _: Int) ->
            pageRequests++
            PESubjectHistoryResponse(
                data = if (page == 1) {
                    PESubjectHistoryData(dataList = listOf(item("t1"), item("t2")), totalRows = 3)
                } else {
                    PESubjectHistoryData(dataList = listOf(item("t3")), totalRows = 3)
                }
            )
        }

        val result = useCase(schoolYear = "2025-2026", subjectId = "S1")

        assertEquals(3, result.size)
        assertEquals(listOf("t3", "t2", "t1"), result.map { it.scoreTime })
        assertEquals(2, pageRequests)
        verifySuspend(VerifyMode.exactly(1)) { gateway.getSubjectScoreHistory("2025-2026", "S1", 2, 50) }
    }

    @Test
    fun `响应data为空时返回空列表`() = runTest {
        everySuspend { gateway.getSubjectScoreHistory(any(), any(), any(), any()) } returns
            PESubjectHistoryResponse(status = "ok")

        assertEquals(emptyList(), useCase(schoolYear = "2025-2026", subjectId = "S1"))
    }

    @Test
    fun `页内无数据时提前终止翻页`() = runTest {
        var pageRequests = 0
        everySuspend { gateway.getSubjectScoreHistory(any(), any(), any(), any()) } calls {
            (_: String, _: String, _: Int, _: Int) ->
            pageRequests++
            PESubjectHistoryResponse(data = PESubjectHistoryData(dataList = emptyList(), totalRows = 0))
        }

        assertEquals(emptyList(), useCase(schoolYear = "2025-2026", subjectId = "S1"))
        assertEquals(1, pageRequests)
    }
}
