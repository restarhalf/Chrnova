@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.exclusion

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.JwxtLoginUseCase
import restarhalf.stellar.schedule.domain.usecase.PELoginUseCase
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.viewmodel.JwxtLoginViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PELoginViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class WelcomeScreenUiTest {

    private val deviceSettings = mock<DeviceSettingsPort>(MockMode.autofill)
    private val jwxtAuthWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    private val appInfo = object : AppInfoPort {
        override val appName = "Chrnova测试"
        override val versionName = "1.0.0"
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    /** WelcomeScreen 内部 koinInject / koinViewModel 依赖整套启动页依赖。 */
    private fun startKoinForTest() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single<AppInfoPort> { appInfo }
                    single<DeviceSettingsPort> { deviceSettings }
                    single { JwxtLoginUseCase(jwxtAuthWorkflow) }
                    single { PELoginUseCase(peAuthWorkflow) }
                    viewModel { JwxtLoginViewModel(get()) }
                    viewModel { PELoginViewModel(get()) }
                }
            )
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 首屏渲染欢迎文案与应用名() = runUltronUiTest {
        startKoinForTest()
        val show = mutableStateOf(true)
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                WelcomeScreen(
                    show = show,
                    pagerState = rememberPagerState(initialPage = 0) { 6 },
                    exitApp = {},
                )
            }
        }
        waitText("欢迎使用")
        onNodeWithText("欢迎使用").assertIsDisplayed()
        onNodeWithText("Chrnova测试").assertIsDisplayed()
        // 首页不显示返回按钮
        assertEquals(0, onAllNodes(hasContentDescription("back")).fetchSemanticsNodes().size)
    }

    @Test
    fun 点击继续进入隐私政策页() = runUltronUiTest {
        startKoinForTest()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                WelcomeScreen(
                    show = mutableStateOf(true),
                    pagerState = rememberPagerState(initialPage = 0) { 6 },
                    exitApp = {},
                )
            }
        }
        // 「继续」是图标的内容描述，不是文本
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasContentDescription("继续")).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithContentDescription("继续").performClick()

        waitText("隐私政策")
        onNodeWithText("隐私政策").assertIsDisplayed()
        onNodeWithText("同意").assertIsDisplayed()
    }

    @Test
    fun 隐私页拒绝会清除确认标记并退出() = runUltronUiTest {
        startKoinForTest()
        var exitCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                WelcomeScreen(
                    show = mutableStateOf(true),
                    pagerState = rememberPagerState(initialPage = 1) { 6 },
                    exitApp = { exitCalls++ },
                )
            }
        }
        waitText("拒绝")
        onNodeWithText("拒绝").performClick()

        waitUntil(timeoutMillis = 3_000) { exitCalls == 1 }
        verify { deviceSettings.setConfirmPrivacy(false) }
    }

    @Test
    fun 非首页显示返回按钮并可回到上一页() = runUltronUiTest {
        startKoinForTest()
        lateinit var pager: PagerState
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                val state = rememberPagerState(initialPage = 1) { 6 }
                pager = state
                WelcomeScreen(show = mutableStateOf(true), pagerState = state, exitApp = {})
            }
        }
        waitText("隐私政策")
        onNodeWithContentDescription("back").performClick()

        waitText("欢迎使用")
        onNodeWithText("欢迎使用").assertIsDisplayed()
        waitUntil(timeoutMillis = 3_000) { pager.currentPage == 0 }
    }

    @Test
    fun 点星页可打开链接并进入完成页() = runUltronUiTest {
        startKoinForTest()
        val show = mutableStateOf(true)
        val openedUris = mutableListOf<String>()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                WelcomeScreen(
                    show = show,
                    pagerState = rememberPagerState(initialPage = 4) { 6 },
                    exitApp = {},
                    openUri = { uri ->
                        openedUris.add(uri)
                        true
                    },
                )
            }
        }
        waitText("去 Github 点星")
        onNodeWithText("去 Github 点星").performClick()
        waitUntil(timeoutMillis = 3_000) { openedUris.isNotEmpty() }
        assertEquals("https://github.com/restarhalf/Chrnova", openedUris[0])

        onNodeWithText("下一步").performClick()
        waitText("设置完成")
        onNodeWithText("进入软件").performClick()
        waitUntil(timeoutMillis = 3_000) { !show.value }
    }
}
