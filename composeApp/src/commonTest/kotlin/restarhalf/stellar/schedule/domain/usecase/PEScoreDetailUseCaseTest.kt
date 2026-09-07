package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.data.local.dao.PEDetailDao
import restarhalf.stellar.schedule.data.local.dao.PEYearScoreDao
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEDetailResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.repository.RoomPERepository
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PEScoreDetailUseCaseTest {

    private val gateway = mock<PEGateway>(MockMode.autofill)
    private val authWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    @Test
    fun `repository为空时observeDetailData抛出状态异常`() {
        val useCase = PEScoreDetailUseCase(gateway, authWorkflow)

        assertFailsWith<IllegalStateException> { useCase.observeDetailData("2025-2026") }
    }

    @Test
    fun `响应无数据时不落库`() = runTest {
        val detailDao = mock<PEDetailDao>(MockMode.autofill)
        val useCase = PEScoreDetailUseCase(
            gateway, authWorkflow, RoomPERepository(mock<PEYearScoreDao>(MockMode.autofill), detailDao)
        )
        everySuspend { gateway.getScoreDetail("2025-2026") } returns PEDetailResponse(status = "ok")

        val result = useCase("2025-2026")

        assertEquals(null, result.data)
        verifySuspend(VerifyMode.not) { detailDao.replaceDetailByYear(any(), any(), any()) }
    }

    @Test
    fun `响应有数据时保存详情`() = runTest {
        val detailDao = mock<PEDetailDao>(MockMode.autofill)
        val useCase = PEScoreDetailUseCase(
            gateway, authWorkflow, RoomPERepository(mock<PEYearScoreDao>(MockMode.autofill), detailDao)
        )
        everySuspend { gateway.getScoreDetail("2025-2026") } returns PEDetailResponse(
            data = PEDetailData(totalScore = 90.0, totalGrade = "优秀")
        )

        val result = useCase("2025-2026")

        assertEquals("优秀", result.data?.totalGrade)
        verifySuspend(VerifyMode.exactly(1)) { detailDao.replaceDetailByYear("2025-2026", any(), any()) }
    }
}
