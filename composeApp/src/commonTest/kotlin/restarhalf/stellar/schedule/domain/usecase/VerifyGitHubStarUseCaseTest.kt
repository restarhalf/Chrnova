package restarhalf.stellar.schedule.domain.usecase

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verify
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerifyGitHubStarUseCaseTest {

    private val papersPort = mock<PapersPort>(MockMode.autofill)
    private val settingsPort = mock<SettingsPort>(MockMode.autofill)
    private val useCase = VerifyGitHubStarUseCase(papersPort, settingsPort)

    @Test
    fun `已点star时写入验证状态并返回true`() = runTest {
        everySuspend { papersPort.verifyStar(any()) } returns true

        val result = useCase("octocat")

        assertTrue(result)
        verify(VerifyMode.exactly(1)) { settingsPort.setStarVerified(true) }
        verifySuspend(VerifyMode.exactly(1)) { papersPort.verifyStar("octocat") }
    }

    @Test
    fun `未点star时不写验证状态并返回false`() = runTest {
        everySuspend { papersPort.verifyStar(any()) } returns false

        val result = useCase("octocat")

        assertFalse(result)
        verify(VerifyMode.not) { settingsPort.setStarVerified(any()) }
    }

    @Test
    fun `验证异常向上抛出`() = runTest {
        everySuspend { papersPort.verifyStar(any()) } throws RuntimeException("network error")

        kotlin.test.assertFailsWith<RuntimeException> { useCase("octocat") }
        verify(VerifyMode.not) { settingsPort.setStarVerified(any()) }
    }
}
