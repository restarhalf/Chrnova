@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class PEAppointmentWebScreenUiTest {

    private val peAuth = mock<PEAuthPort>(MockMode.autofill)

    private val loginHint = "请先登录体测系统，登录后可免登录打开官方预约页。"

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 未登录时渲染登录提示卡片() = runUltronUiTest {
        every { peAuth.getToken() } returns null
        every { peAuth.getUserId() } returns null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentWebScreen(onBack = {}, peAuth = peAuth)
            }
        }
        waitText("体测预约")
        onNodeWithText("体测预约").assertIsDisplayed()
        onNodeWithText(loginHint).assertIsDisplayed()
    }

    @Test
    fun 只有token没有userId时仍提示登录() = runUltronUiTest {
        every { peAuth.getToken() } returns "tok"
        every { peAuth.getUserId() } returns ""
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentWebScreen(onBack = {}, peAuth = peAuth)
            }
        }
        waitText(loginHint)
        onNodeWithText(loginHint).assertIsDisplayed()
    }

    @Test
    fun 已登录时不再显示登录提示() = runUltronUiTest {
        every { peAuth.getToken() } returns "tok"
        every { peAuth.getUserId() } returns "u1"
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentWebScreen(onBack = {}, peAuth = peAuth)
            }
        }
        waitText("体测预约")

        assertEquals(0, onAllNodesWithText(loginHint).fetchSemanticsNodes().size)
    }

    @Test
    fun 返回按钮回调() = runUltronUiTest {
        every { peAuth.getToken() } returns null
        every { peAuth.getUserId() } returns null
        var backCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentWebScreen(onBack = { backCalls++ }, peAuth = peAuth)
            }
        }
        waitText("体测预约")
        onNodeWithContentDescription("返回").performClick()
        waitUntil(timeoutMillis = 2_000) { backCalls == 1 }
        assertEquals(1, backCalls)
    }
}
