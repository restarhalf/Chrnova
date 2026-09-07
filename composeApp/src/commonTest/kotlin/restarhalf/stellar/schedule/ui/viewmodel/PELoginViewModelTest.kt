package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.PELoginUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * PELoginViewModel 单元测试。
 *
 * PELoginUseCase 是 final class 无法 mock，用真实实例 + mock PEAuthWorkflowPort。
 * uiState 是裸 MutableStateFlow(asStateFlow)，无需订阅即可读 value；
 * viewModelScope 内挂起流程用类级 UnconfinedTestDispatcher 的 scheduler drain。
 */
class PELoginViewModelTest {

    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun makeViewModel() = PELoginViewModel(
        peLoginUseCase = PELoginUseCase(peAuthWorkflow),
    )

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `输入用户名更新uiState`() = runTest {
        val vm = makeViewModel()

        vm.onUsernameChange("2023001")

        assertEquals("2023001", vm.uiState.value.username)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `输入密码更新uiState`() = runTest {
        val vm = makeViewModel()

        vm.onPasswordChange("pwd")

        assertEquals("pwd", vm.uiState.value.password)
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `输入变更清空已有错误提示`() = runTest {
        everySuspend {
            peAuthWorkflow.login(any(), any())
        } throws RuntimeException("密码错误")

        val vm = makeViewModel()
        vm.onUsernameChange("2023001")
        vm.onPasswordChange("pwd")
        vm.submitLogin { }
        advanceMain()
        assertTrue(vm.uiState.value.error != null)

        vm.onUsernameChange("2023002")
        assertEquals(null, vm.uiState.value.error)

        vm.onPasswordChange("pwd2")
        assertEquals(null, vm.uiState.value.error)
    }

    @Test
    fun `用户名为空时提交不触发登录`() = runTest {
        val vm = makeViewModel()
        var successCalls = 0

        vm.submitLogin { successCalls++ }
        advanceMain()

        assertEquals(0, successCalls)
        assertFalse(vm.uiState.value.loading)
        verifySuspend(VerifyMode.exactly(0)) {
            peAuthWorkflow.login(any(), any())
        }
    }

    @Test
    fun `密码为空时提交不触发登录`() = runTest {
        val vm = makeViewModel()
        vm.onUsernameChange("2023001")
        var successCalls = 0

        vm.submitLogin { successCalls++ }
        advanceMain()

        assertEquals(0, successCalls)
        assertFalse(vm.uiState.value.loading)
        verifySuspend(VerifyMode.exactly(0)) {
            peAuthWorkflow.login(any(), any())
        }
    }

    @Test
    fun `登录成功清空密码并回调onSuccess`() = runTest {
        everySuspend {
            peAuthWorkflow.login(any(), any())
        } returns Unit

        val vm = makeViewModel()
        vm.onUsernameChange("2023001")
        vm.onPasswordChange("pwd")
        var successCalls = 0

        vm.submitLogin { successCalls++ }
        advanceMain()

        assertEquals(1, successCalls)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.loading)
        assertEquals(null, vm.uiState.value.error)
        assertEquals("2023001", vm.uiState.value.username)
    }

    @Test
    fun `登录失败透传异常message并复位loading`() = runTest {
        everySuspend {
            peAuthWorkflow.login(any(), any())
        } throws RuntimeException("密码错误")

        val vm = makeViewModel()
        vm.onUsernameChange("2023001")
        vm.onPasswordChange("pwd")
        var successCalls = 0

        vm.submitLogin { successCalls++ }
        advanceMain()

        assertEquals(0, successCalls)
        assertFalse(vm.uiState.value.loading)
        assertEquals("密码错误", vm.uiState.value.error)
        assertEquals("pwd", vm.uiState.value.password)
    }

    @Test
    fun `异常无message时使用默认文案`() = runTest {
        everySuspend {
            peAuthWorkflow.login(any(), any())
        } throws RuntimeException()

        val vm = makeViewModel()
        vm.onUsernameChange("2023001")
        vm.onPasswordChange("pwd")

        vm.submitLogin { }
        advanceMain()

        assertEquals("登录失败", vm.uiState.value.error)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `提交时用户名trim后透传`() = runTest {
        everySuspend {
            peAuthWorkflow.login(any(), any())
        } returns Unit

        val vm = makeViewModel()
        vm.onUsernameChange(" 2023001 ")
        vm.onPasswordChange("pwd")

        vm.submitLogin { }
        advanceMain()

        verifySuspend(VerifyMode.exactly(1)) {
            peAuthWorkflow.login(username = "2023001", password = "pwd")
        }
    }
}
