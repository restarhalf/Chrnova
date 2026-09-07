@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.JwxtLoginUseCase
import restarhalf.stellar.schedule.ui.viewmodel.JwxtLoginViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 教务登录页 Compose UI 测试（Ultron KMP 入口）。
 *
 * VM 构造同 JVM 测试配方：JwxtLoginUseCase 真实实例 + mock JwxtAuthWorkflowPort
 * （login 5 参 suspend，成功返回 Unit）。空凭据按钮禁用 → 点击无回调。
 */
class JwxtLoginScreenUiTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)

    private fun makeViewModel() = JwxtLoginViewModel(
        jwxtLoginUseCase = JwxtLoginUseCase(authWorkflow),
    )

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染登录页与空凭据门禁() = runUltronUiTest {
        val vm = makeViewModel()
        var successCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                JwxtLoginScreen(
                    vm = vm,
                    onBack = {},
                    onLoginSuccess = { successCalls++ },
                    inWel = false,
                    next = {},
                )
            }
        }
        waitText("登录教务系统账号")
        onNodeWithText("账号").assertExists()
        onNodeWithText("密码").assertExists()
        // 空凭据 → 登录按钮禁用，点击无回调
        onNodeWithText("登录").performClick()
        Thread.sleep(500)
        assertEquals(0, successCalls)
        // 填写凭据后点击登录 → 成功回调触发
        everySuspend { authWorkflow.login(any(), any(), any(), any(), any()) } returns Unit
        vm.onUserNoChange("2023001")
        vm.onPasswordChange("pwd")
        runOnIdle { }
        onNodeWithText("登录").performClick()
        waitUntil(timeoutMillis = 3_000) { successCalls == 1 }
    }
}
