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
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.JwxtLoginUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JwxtLoginViewModel 单元测试。
 *
 * JwxtLoginUseCase 是 final class 无法 mock，用真实实例 + mock JwxtAuthWorkflowPort。
 * uiState 是裸 MutableStateFlow(asStateFlow)，无需订阅即可读 value；
 * viewModelScope 内挂起流程用类级 UnconfinedTestDispatcher 的 scheduler drain。
 */
class JwxtLoginViewModelTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun makeViewModel() = JwxtLoginViewModel(
        jwxtLoginUseCase = JwxtLoginUseCase(authWorkflow),
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
    fun `输入学号更新uiState`() = runTest {
        val vm = makeViewModel()

        vm.onUserNoChange("2023001")

        assertEquals("2023001", vm.uiState.value.userNo)
        assertEquals("", vm.uiState.value.error)
    }

    @Test
    fun `输入密码更新uiState`() = runTest {
        val vm = makeViewModel()

        vm.onPasswordChange("pwd")

        assertEquals("pwd", vm.uiState.value.password)
        assertEquals("", vm.uiState.value.error)
    }

    @Test
    fun `输入变更清空已有错误提示`() = runTest {
        everySuspend {
            authWorkflow.login(any(), any(), any(), any(), any())
        } throws RuntimeException("验证码错误")

        val vm = makeViewModel()
        vm.onUserNoChange("2023001")
        vm.onPasswordChange("pwd")
        vm.submitLogin { }
        advanceMain()
        assertTrue(vm.uiState.value.error.isNotEmpty())

        vm.onUserNoChange("2023002")
        assertEquals("", vm.uiState.value.error)

        vm.onPasswordChange("pwd2")
        assertEquals("", vm.uiState.value.error)
    }

    @Test
    fun `学号为空时提交不触发登录`() = runTest {
        val vm = makeViewModel()
        var successCalls = 0

        vm.submitLogin { successCalls++ }
        advanceMain()

        assertEquals(0, successCalls)
        assertFalse(vm.uiState.value.loading)
        verifySuspend(VerifyMode.exactly(0)) {
            authWorkflow.login(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `密码为空时提交不触发登录`() = runTest {
        val vm = makeViewModel()
        vm.onUserNoChange("2023001")
        var successCalls = 0

        vm.submitLogin { successCalls++ }
        advanceMain()

        assertEquals(0, successCalls)
        assertFalse(vm.uiState.value.loading)
        verifySuspend(VerifyMode.exactly(0)) {
            authWorkflow.login(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `登录成功清空密码并回调onSuccess`() = runTest {
        everySuspend {
            authWorkflow.login(any(), any(), any(), any(), any())
        } returns Unit

        val vm = makeViewModel()
        vm.onUserNoChange("2023001")
        vm.onPasswordChange("pwd")
        var successCalls = 0

        vm.submitLogin { successCalls++ }
        advanceMain()

        assertEquals(1, successCalls)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.loading)
        assertEquals("", vm.uiState.value.error)
        assertEquals("2023001", vm.uiState.value.userNo)
    }

    @Test
    fun `登录失败置错误信息并复位loading`() = runTest {
        everySuspend {
            authWorkflow.login(any(), any(), any(), any(), any())
        } throws RuntimeException("验证码错误")

        val vm = makeViewModel()
        vm.onUserNoChange("2023001")
        vm.onPasswordChange("pwd")
        var successCalls = 0

        vm.submitLogin { successCalls++ }
        advanceMain()

        assertEquals(0, successCalls)
        assertFalse(vm.uiState.value.loading)
        assertTrue(vm.uiState.value.error.isNotEmpty())
        assertEquals("pwd", vm.uiState.value.password)
    }

    @Test
    fun `提交时学号trim后透传`() = runTest {
        everySuspend {
            authWorkflow.login(any(), any(), any(), any(), any())
        } returns Unit

        val vm = makeViewModel()
        vm.onUserNoChange(" 2023001 ")
        vm.onPasswordChange("pwd")

        vm.submitLogin { }
        advanceMain()

        verifySuspend(VerifyMode.exactly(1)) {
            authWorkflow.login(
                userNo = "2023001",
                password = "pwd",
                captchaData = "",
                codeVal = "",
                p = null,
            )
        }
    }
}
