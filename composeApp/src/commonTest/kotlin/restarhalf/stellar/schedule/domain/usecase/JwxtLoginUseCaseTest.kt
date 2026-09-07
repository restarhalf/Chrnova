package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwxtLoginUseCaseTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val useCase = JwxtLoginUseCase(authWorkflow)

    @Test
    fun `登录参数完整透传`() = runTest {
        useCase(userNo = "2023001", password = "pwd", captchaData = "cap", codeVal = "1234", p = "pk")

        verifySuspend(VerifyMode.exactly(1)) {
            authWorkflow.login(
                userNo = "2023001",
                password = "pwd",
                captchaData = "cap",
                codeVal = "1234",
                p = "pk",
            )
        }
    }

    @Test
    fun `登录失败时异常原样重抛`() = runTest {
        everySuspend {
            authWorkflow.login(any(), any(), any(), any(), any())
        } throws RuntimeException("验证码错误")

        val ex = assertFailsWith<RuntimeException> {
            useCase(userNo = "u", password = "p")
        }
        assertEquals("验证码错误", ex.message)
    }
}
