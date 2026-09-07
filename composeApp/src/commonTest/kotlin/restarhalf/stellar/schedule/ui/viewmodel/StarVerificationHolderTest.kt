package restarhalf.stellar.schedule.ui.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * StarVerificationHolder 单元测试。
 *
 * 真实 VerifyGitHubStarUseCase 桥接 mock PapersPort/SettingsPort（final class 不可 mock）。
 * scope 用类级 UnconfinedTestDispatcher，verify() 内 launch 立即执行。
 */
class StarVerificationHolderTest {

    private val papersPort = mock<PapersPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun makeHolder(verified: Boolean = false): StarVerificationHolder {
        every { settings.getStarVerified() } returns verified
        return StarVerificationHolder(
            verifyGitHubStar = VerifyGitHubStarUseCase(papersPort, settings),
            settings = settings,
            scope = CoroutineScope(mainDispatcher),
        )
    }

    @Test
    fun `init已验证直接置isVerified`() = runTest {
        val holder = makeHolder(verified = true)

        assertTrue(holder.state.value.isVerified)
        assertFalse(holder.state.value.showDialog)
    }

    @Test
    fun `init未验证为默认状态`() = runTest {
        val holder = makeHolder()

        assertFalse(holder.state.value.isVerified)
        assertFalse(holder.state.value.showDialog)
        assertFalse(holder.state.value.isVerifying)
        assertNull(holder.state.value.error)
    }

    @Test
    fun `showDialog开启且dismiss重置输入与错误`() = runTest {
        val holder = makeHolder()
        holder.showDialog()
        assertTrue(holder.state.value.showDialog)

        holder.onUsernameChange(TextFieldValue("someone"))
        holder.verify() // 未 stub verifyStar → 不重要，先改输入再关
        holder.dismissDialog()

        val state = holder.state.value
        assertFalse(state.showDialog)
        assertEquals(TextFieldValue(), state.username)
        assertNull(state.error)
        assertFalse(state.isVerifying)
    }

    @Test
    fun `verify空白用户名不发起请求`() = runTest {
        val holder = makeHolder()
        holder.showDialog()
        holder.onUsernameChange(TextFieldValue("   "))

        holder.verify()

        val state = holder.state.value
        assertFalse(state.isVerifying)
        assertNull(state.error)
        verifySuspend(VerifyMode.exactly(0)) { papersPort.verifyStar(any()) }
    }

    @Test
    fun `verify成功置已验证并持久化`() = runTest {
        everySuspend { papersPort.verifyStar("someone") } returns true
        val holder = makeHolder()
        holder.showDialog()
        holder.onUsernameChange(TextFieldValue(" someone "))

        holder.verify()

        val state = holder.state.value
        assertTrue(state.isVerified)
        assertFalse(state.showDialog)
        assertFalse(state.isVerifying)
        assertNull(state.error)
        verify(VerifyMode.exactly(1)) { settings.setStarVerified(true) }
    }

    @Test
    fun `verify未star保留对话框并提示`() = runTest {
        everySuspend { papersPort.verifyStar("someone") } returns false
        val holder = makeHolder()
        holder.showDialog()
        holder.onUsernameChange(TextFieldValue("someone"))

        holder.verify()

        val state = holder.state.value
        assertFalse(state.isVerified)
        assertTrue(state.showDialog)
        assertFalse(state.isVerifying)
        assertEquals("未检测到 star，请先 star 仓库后再试", state.error)
        verify(VerifyMode.exactly(0)) { settings.setStarVerified(true) }
    }

    @Test
    fun `verify异常置错误信息`() = runTest {
        everySuspend { papersPort.verifyStar("someone") } throws RuntimeException("网络超时")
        val holder = makeHolder()
        holder.showDialog()
        holder.onUsernameChange(TextFieldValue("someone"))

        holder.verify()

        val state = holder.state.value
        assertFalse(state.isVerifying)
        assertFalse(state.isVerified)
        assertEquals("网络超时", state.error)
        verifySuspend(VerifyMode.exactly(1)) { papersPort.verifyStar("someone") }
    }
}
