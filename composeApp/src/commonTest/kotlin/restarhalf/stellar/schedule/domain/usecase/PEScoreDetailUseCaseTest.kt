package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.model.PEDetailData
import restarhalf.stellar.schedule.domain.model.PEDetailResponse
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.repository.PERepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PEScoreDetailUseCaseTest {

    private val gateway = mock<PEGateway>(MockMode.autofill)
    private val authWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)
    private val repository = mock<PERepository>(MockMode.autofill)

    @Test
    fun `repository为空时observeDetailData抛出状态异常`() {
        val useCase = PEScoreDetailUseCase(gateway, authWorkflow)

        assertFailsWith<IllegalStateException> { useCase.observeDetailData("2025-2026") }
    }

    @Test
    fun `响应无数据时不落库`() = runTest {
        val useCase = PEScoreDetailUseCase(gateway, authWorkflow, repository)
        everySuspend { gateway.getScoreDetail("2025-2026") } returns PEDetailResponse(status = "ok")

        val result = useCase("2025-2026")

        assertEquals(null, result.data)
        verifySuspend(VerifyMode.not) { repository.saveDetailData(any(), any()) }
    }

    @Test
    fun `响应有数据时保存详情`() = runTest {
        val useCase = PEScoreDetailUseCase(gateway, authWorkflow, repository)
        everySuspend { gateway.getScoreDetail("2025-2026") } returns PEDetailResponse(
            data = PEDetailData(totalScore = 90.0, totalGrade = "优秀")
        )

        val result = useCase("2025-2026")

        assertEquals("优秀", result.data?.totalGrade)
        verifySuspend(VerifyMode.exactly(1)) { repository.saveDetailData("2025-2026", any()) }
    }
}
