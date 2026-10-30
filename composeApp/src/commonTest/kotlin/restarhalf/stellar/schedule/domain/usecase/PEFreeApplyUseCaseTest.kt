package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.error.PETokenExpiredException
import restarhalf.stellar.schedule.domain.model.PEFreeApplyListResponse
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PEFreeApplyUseCaseTest {

    private val gateway = mock<PEGateway>(MockMode.autofill)
    private val authWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)
    private val useCase = PEFreeApplyUseCase(gateway, authWorkflow)

    @Test
    fun `提交申请遇到认证错误只刷新不重放`() = runTest {
        everySubmitThrowsExpired()

        assertFailsWith<PETokenExpiredException> {
            useCase.apply(
                stdNumber = "2023001",
                schoolYear = "2025",
                freeApplyType = "free",
                attachments = listOf("att-1"),
            )
        }

        verifySuspend(VerifyMode.exactly(1)) { gateway.submitFreeApply(any(), any(), any(), any()) }
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
    }

    @Test
    fun `上传附件遇到认证错误只刷新不重放`() = runTest {
        everySuspend { gateway.uploadPeFile(any(), any(), any()) } throws PETokenExpiredException()

        assertFailsWith<PETokenExpiredException> {
            useCase.upload(fileName = "a.pdf", mimeType = "application/pdf", bytes = byteArrayOf(1))
        }

        verifySuspend(VerifyMode.exactly(1)) { gateway.uploadPeFile(any(), any(), any()) }
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
    }

    @Test
    fun `读取列表遇到认证错误刷新后重试`() = runTest {
        var attempts = 0
        everySuspend { gateway.getFreeApplyList() } calls {
            attempts++
            if (attempts == 1) throw PETokenExpiredException()
            PEFreeApplyListResponse(status = "PASS")
        }

        val response = useCase.list()

        assertEquals("PASS", response.status)
        assertEquals(2, attempts)
        verifySuspend(VerifyMode.exactly(1)) { authWorkflow.refreshSession() }
    }

    private fun everySubmitThrowsExpired() {
        everySuspend { gateway.submitFreeApply(any(), any(), any(), any()) } throws
            PETokenExpiredException()
    }
}
