@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.ui.viewmodel.PersonalInfoViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 个人资料页 Compose UI 测试（Ultron KMP 入口）。
 *
 * PersonalInfoViewModel 手动构造（settingsPort mock，VM init 读取头像/昵称）。
 * 两个退出确认对话框均为 WindowDialog 独立窗口（主窗口语义树不可见），
 * 仅断言门控行为："退出登录"入口存在、未登录分区不渲染。
 */
class ProfileScreenUiTest {

    private val settingsPort = mock<SettingsPort>(MockMode.autofill)

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染个人信息与教务系统分区() = runUltronUiTest {
        every { settingsPort.getUserAvatarUri() } returns null
        every { settingsPort.getUserNickname() } returns "小明"
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ProfileScreen(
                    peAuthProfile = null,
                    jwxtAuthProfile = JwxtAuthProfile(
                        name = "张三",
                        userNo = "2023001",
                        clsName = "软件2101",
                        academyName = "信息学院",
                    ),
                    onBack = {},
                    personalInfoViewModel = PersonalInfoViewModel(settingsPort),
                )
            }
        }
        waitText("个人资料")
        onNodeWithText("个人信息").assertExists()
        onNodeWithText("小明").assertExists()
        onNodeWithText("教务系统").assertExists()
        onNodeWithText("张三").assertExists()
        onNodeWithText("2023001").assertExists()
        onNodeWithText("软件2101").assertExists()
        onNodeWithText("信息学院").assertExists()
        onNodeWithText("退出登录").assertExists()
    }

    @Test
    fun 体测平台分区未登录不渲染() = runUltronUiTest {
        every { settingsPort.getUserAvatarUri() } returns null
        every { settingsPort.getUserNickname() } returns null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ProfileScreen(
                    peAuthProfile = PEAuthProfile(stuName = "", stdNumber = ""),
                    jwxtAuthProfile = JwxtAuthProfile(name = "张三", userNo = "2023001"),
                    onBack = {},
                    personalInfoViewModel = PersonalInfoViewModel(settingsPort),
                )
            }
        }
        waitText("教务系统")
        // stdNumber 为空 → isPeLoggedIn=false → 体测平台分区不渲染
        assertEquals(0, onAllNodesWithText("体测平台").fetchSemanticsNodes().size)
    }
}
