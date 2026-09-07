@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.atiurin.ultron.core.compose.runUltronUiTest
import androidx.compose.ui.semantics.SemanticsActions
import dev.mokkery.MockMode
import dev.mokkery.every
import dev.mokkery.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.domain.usecase.CheckAppUpdateUseCase
import restarhalf.stellar.schedule.ui.blur.LocalBlurEnabled
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import restarhalf.stellar.schedule.ui.viewmodel.AboutViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 关于页 Compose UI 测试（Ultron KMP 入口）。
 *
 * AppInfoPort 经 koinInject 获取：测试内 startKoin（已有 Koin 时 loadKoinModules 补注册，
 * 与 ExclusionScreenUiTest 的 Koin 共存）。
 * 应用版本 5 连击彩蛋用语义 OnClick 直调，避免触碰注入与布局同步的竞态。
 */
class AboutScreenUiTest {

    private val appUpdate = mock<AppUpdatePort>(MockMode.autofill)

    private val appInfo = object : AppInfoPort {
        override val appName: String = "Chrnova"
        override val versionName: String = "1.0.0"
    }

    private companion object {
        @Volatile
        private var koinReady = false
    }

    private fun ensureKoin() {
        if (koinReady) return
        val module = module { single<AppInfoPort> { appInfo } }
        if (GlobalContext.getOrNull() == null) {
            startKoin { modules(module) }
        } else {
            loadKoinModules(module)
        }
        koinReady = true
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染应用信息与功能入口() = runUltronUiTest {
        ensureKoin()
        val vm = AboutViewModel(checkAppUpdate = CheckAppUpdateUseCase(appUpdate))
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                // 关闭动态背景效果（RuntimeShader 无限动画会让 idle 永不满足）
                CompositionLocalProvider(LocalBlurEnabled provides false) {
                    AboutScreen(vm = vm, onBack = {})
                }
            }
        }
        waitText("应用信息")
        onAllNodesWithText("应用版本")[0].assertExists()
        onNodeWithText("检查更新").assertExists()
        onNodeWithText("功能").assertExists()
        onNodeWithText("加入 QQ 群").assertExists()
        onNodeWithText("打开教务系统").assertExists()
        onNodeWithText("赞赏作者").assertExists()
        onNodeWithText("项目").assertExists()
        onNodeWithText("github.com/restarhalf/Chrnova").assertExists()
    }

    @Test
    fun 应用版本五连击触发彩蛋回调() = runUltronUiTest {
        ensureKoin()
        val vm = AboutViewModel(checkAppUpdate = CheckAppUpdateUseCase(appUpdate))
        var iconTaps = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CompositionLocalProvider(LocalBlurEnabled provides false) {
                    AboutScreen(vm = vm, onBack = {}, onIconTap = { iconTaps++ })
                }
            }
        }
        waitText("应用信息")
        repeat(5) {
            onNodeWithText("应用版本").performSemanticsAction(SemanticsActions.OnClick)
        }
        waitUntil(timeoutMillis = 3_000) { iconTaps == 1 }
        assertEquals(1, iconTaps)
    }
}
