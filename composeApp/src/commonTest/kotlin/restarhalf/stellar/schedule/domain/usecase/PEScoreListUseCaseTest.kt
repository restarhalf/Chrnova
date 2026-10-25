package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.PEScoreListResponse
import restarhalf.stellar.schedule.domain.model.PEYearScore
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.repository.PERepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PEScoreListUseCaseTest {

    private val gateway = mock<PEGateway>(MockMode.autofill)
    private val authWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)
    private val repository = mock<PERepository>(MockMode.autofill)

    @Test
    fun `repository为空时observeScoreList抛出状态异常`() {
        val useCase = PEScoreListUseCase(gateway, authWorkflow)

        assertFailsWith<IllegalStateException> { useCase.observeScoreList() }
    }

    @Test
    fun `获取成绩列表透传响应`() = runTest {
        val useCase = PEScoreListUseCase(gateway, authWorkflow)
        val response = PEScoreListResponse(
            dataArr = listOf(PEYearScore(schoolYear = "2025-2026", total = 95.0))
        )
        everySuspend { gateway.getScoreList() } returns response

        val result = useCase()

        assertEquals(response, result)
    }

    @Test
    fun `repository非空时替换本地成绩`() = runTest {
        val response = PEScoreListResponse(
            dataArr = listOf(PEYearScore(schoolYear = "2025-2026", total = 95.0))
        )
        val useCase = PEScoreListUseCase(gateway, authWorkflow, repository)
        everySuspend { gateway.getScoreList() } returns response

        val result = useCase()

        assertEquals(1, result.dataArr.size)
        verifySuspend(VerifyMode.exactly(1)) { repository.replaceScores(any()) }
    }
}
