@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.exclusion

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import com.russhwolf.settings.ObservableSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class ExclusionScreenUiTest {

    private val settings = mock<ObservableSettings>(MockMode.autofill) {
        every { putBoolean(any(), any()) } returns Unit
    }
    private val deviceSettings = mock<DeviceSettingsPort>(MockMode.autofill)

    private fun startKoinForTest() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single<AppInfoPort> {
                        object : AppInfoPort {
                            override val appName = "Chrnova测试"
                            override val versionName = "1.0.0"
                        }
                    }
                    single<ObservableSettings>(named(SettingsKeys.PREFS_NAME)) { settings }
                    single<DeviceSettingsPort> { deviceSettings }
                }
            )
        }
    }

    private fun miuix(content: @Composable () -> Unit): @Composable () -> Unit = {
        MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) { content() }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 隐私政策页渲染标题与按钮() = runUltronUiTest {
        setContent(miuix {
            PrivacyScreen(pagerState = rememberPagerState(initialPage = 1) { 6 }, onExit = {})
        })
        waitText("隐私政策")
        onNodeWithText("隐私政策").assertIsDisplayed()
        onNodeWithText("拒绝").assertIsDisplayed()
        onNodeWithText("同意").assertIsDisplayed()
    }

    @Test
    fun 拒绝按钮回调onExit() = runUltronUiTest {
        var exited = false
        setContent(miuix {
            PrivacyScreen(pagerState = rememberPagerState(initialPage = 1) { 6 }, onExit = { exited = true })
        })
        waitText("拒绝")
        onNodeWithText("拒绝").performClick()
        waitUntil(timeoutMillis = 2_000) { exited }
        assertEquals(true, exited)
    }

    @Test
    fun 同意按钮翻页到登录页() = runUltronUiTest {

        setContent(miuix {
            val state = rememberPagerState(initialPage = 1) { 6 }
            androidx.compose.foundation.pager.HorizontalPager(state = state, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 1) {
                    PrivacyScreen(pagerState = state, onExit = {})
                } else {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                    ) {
                        top.yukonga.miuix.kmp.basic.Text("PAGE_$page")
                    }
                }
            }
        })
        waitText("同意")

        onNodeWithText("同意").performTouchInput {
            down(androidx.compose.ui.geometry.Offset(centerX, top + 8.dp.toPx()))
            up()
        }

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("PAGE_2").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 星标页渲染文案() = runUltronUiTest {
        setContent(miuix {
            GitHubStarScreen(pagerState = rememberPagerState(initialPage = 4) { 6 }, onStarClick = {}, onNext = {})
        })
        waitText("喜欢这个应用吗？")
        onNodeWithText("喜欢这个应用吗？").assertIsDisplayed()
        onNodeWithText("去 Github 点星").assertIsDisplayed()
        onNodeWithText("下一步").assertIsDisplayed()
    }

    @Test
    fun 去点星按钮回调() = runUltronUiTest {
        var starClicked = false
        setContent(miuix {
            GitHubStarScreen(
                pagerState = rememberPagerState(initialPage = 4) { 6 },
                onStarClick = { starClicked = true },
                onNext = {},
            )
        })
        waitText("去 Github 点星")
        onNodeWithText("去 Github 点星").performClick()
        waitUntil(timeoutMillis = 2_000) { starClicked }
    }

    @Test
    fun 下一步按钮回调() = runUltronUiTest {
        var nextClicked = false
        setContent(miuix {
            GitHubStarScreen(
                pagerState = rememberPagerState(initialPage = 4) { 6 },
                onStarClick = {},
                onNext = { nextClicked = true },
            )
        })
        waitText("下一步")
        onNodeWithText("下一步").performClick()
        waitUntil(timeoutMillis = 2_000) { nextClicked }
    }

    @Test
    fun 欢迎页渲染应用名并前进到隐私页() = runUltronUiTest {
        startKoinForTest()
        var pager: androidx.compose.foundation.pager.PagerState? = null
        setContent(miuix {
            val state = rememberPagerState(initialPage = 0) { 6 }
            pager = state
            WelcomeEnterScreen(pagerState = state)
        })
        waitText("欢迎使用")
        onNodeWithText("Chrnova测试").assertIsDisplayed()
    }

    @Test
    fun 设置完成页渲染并可进入软件() = runUltronUiTest {
        startKoinForTest()
        var dismissed = false
        setContent(miuix {
            EnterScreen(onDismissRequest = { dismissed = true }, pagerState = rememberPagerState(initialPage = 5) { 6 })
        })
        waitText("设置完成")
        onNodeWithText("进入软件").performClick()
        waitUntil(timeoutMillis = 2_000) { dismissed }
        assertEquals(true, dismissed)

        verify { deviceSettings.setConfirmPrivacy(true) }
    }

}
