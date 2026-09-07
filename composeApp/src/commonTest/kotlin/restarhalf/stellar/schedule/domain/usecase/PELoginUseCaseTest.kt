package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PELoginUseCaseTest {

    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)
    private val useCase = PELoginUseCase(peAuthWorkflow)

    @Test
    fun `登录参数透传到认证工作流`() = runTest {
        useCase(username = "2023001", password = "pwd123")

        verifySuspend(VerifyMode.exactly(1)) { peAuthWorkflow.login("2023001", "pwd123") }
    }

    @Test
    fun `登录异常原样重抛`() = runTest {
        everySuspend { peAuthWorkflow.login(any(), any()) } throws RuntimeException("invalid credentials")

        assertFailsWith<RuntimeException> { useCase("2023001", "bad") }
    }
}
