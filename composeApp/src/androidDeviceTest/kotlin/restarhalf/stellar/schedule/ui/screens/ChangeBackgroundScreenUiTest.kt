@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 更换背景页 Compose UI 测试（Ultron KMP 入口）。
 *
 * BackgroundViewModel(port) 手动构造，observe 流用 MutableStateFlow 模拟，
 * 可验证 imageUri 变化驱动的响应式 UI（纯色/自定义切换 + 清除图片项出现）。
 */
class ChangeBackgroundScreenUiTest {

    private val port = mock<BackgroundSettingsPort>(MockMode.autofill)

    private val imageUriFlow = MutableStateFlow<String?>(null)
    private val alphaFlow = MutableStateFlow(0.6f)
    private val blurFlow = MutableStateFlow(0.2f)
    private val componentsAlphaFlow = MutableStateFlow(0.8f)

    private fun stubFlows() {
        every { port.observeBackgroundImageUri() } returns imageUriFlow
        every { port.observeBackgroundAlpha() } returns alphaFlow
        every { port.observeBackgroundBlur() } returns blurFlow
        every { port.observeComponentsAlpha() } returns componentsAlphaFlow
        // setter 回写 observe 流，模拟真实设置的响应式链路
        every { port.setBackgroundImageUri(any()) } calls { (uri: String?) -> imageUriFlow.value = uri }
    }

    private fun makeViewModel(): BackgroundViewModel {
        stubFlows()
        return BackgroundViewModel(port)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染图片与效果调节项() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ChangeBackgroundScreen(vm = makeViewModel(), onBack = {})
            }
        }
        waitText("背景图片")
        onNodeWithText("选择图片").assertExists()
        onNodeWithText("当前：纯色背景").assertExists()
        onNodeWithText("效果调节").assertExists()
        onNodeWithText("背景透明度").assertExists()
        onNodeWithText("60%").assertExists()
        onNodeWithText("背景模糊度").assertExists()
        onNodeWithText("20%").assertExists()
        onNodeWithText("组件透明度").assertExists()
        onNodeWithText("80%").assertExists()
    }

    @Test
    fun 设置背景图后切换为自定义并出现清除项() = runUltronUiTest {
        val vm = makeViewModel()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ChangeBackgroundScreen(vm = vm, onBack = {})
            }
        }
        waitText("当前：纯色背景")
        imageUriFlow.value = "content://bg.png"
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("当前：自定义背景").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("清除图片").assertExists()
        onNodeWithText("恢复为纯色背景").assertExists()
        // 清除图片走 VM 回写 null
        onNodeWithText("清除图片").performSemanticsAction(SemanticsActions.OnClick)
        waitUntil(timeoutMillis = 3_000) { imageUriFlow.value == null }
        assertEquals(null, imageUriFlow.value)
    }
}
