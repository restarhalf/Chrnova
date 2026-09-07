package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import restarhalf.stellar.schedule.data.remote.PETokenExpiredException
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PESessionRetryTest {

    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    @Test
    fun `首次成功不刷新会话`() = runTest {
        val result = withSessionRetry(peAuthWorkflow) { "ok" }

        assertEquals("ok", result)
        verifySuspend(VerifyMode.not) { peAuthWorkflow.refreshSession() }
    }

    @Test
    fun `令牌过期刷新会话后重试成功`() = runTest {
        var attempts = 0
        val result = withSessionRetry(peAuthWorkflow) {
            attempts++
            if (attempts == 1) throw PETokenExpiredException()
            "retried"
        }

        assertEquals("retried", result)
        assertEquals(2, attempts)
        verifySuspend(VerifyMode.exactly(1)) { peAuthWorkflow.refreshSession() }
    }

    @Test
    fun `解析异常刷新会话后重试成功`() = runTest {
        var attempts = 0
        val result = withSessionRetry(peAuthWorkflow) {
            attempts++
            if (attempts == 1) throw SerializationException("bad json")
            42
        }

        assertEquals(42, result)
        assertEquals(2, attempts)
        verifySuspend(VerifyMode.exactly(1)) { peAuthWorkflow.refreshSession() }
    }

    @Test
    fun `其他异常直接抛出不刷新`() = runTest {
        assertFailsWith<IllegalStateException> {
            withSessionRetry(peAuthWorkflow) { throw IllegalStateException("boom") }
        }
        verifySuspend(VerifyMode.not) { peAuthWorkflow.refreshSession() }
    }

    @Test
    fun `重试仍失败时异常向上抛出`() = runTest {
        var attempts = 0

        assertFailsWith<PETokenExpiredException> {
            withSessionRetry(peAuthWorkflow) {
                attempts++
                throw PETokenExpiredException()
            }
        }

        assertEquals(2, attempts)
    }
}
