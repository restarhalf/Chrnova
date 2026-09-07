@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import com.atiurin.ultron.core.compose.runUltronUiTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.ui.port.AppInfoPort
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 日志页 Compose UI 测试（Ultron KMP 入口）。
 *
 * LogScreen 无 VM：日志数据来自 AppLogger 单例 StateFlow，测试前写入条目。
 * koinInject(AppInfoPort) 与 AboutScreenUiTest 共享进程级 Koin（loadKoinModules 兜底）。
 * 清空确认对话框是 WindowDialog 独立窗口（主窗口语义树不可见），仅断言按钮存在。
 */
class LogScreenUiTest {

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
    fun 渲染日志条目与操作按钮() = runUltronUiTest {
        ensureKoin()
        // AppLogger.enabled 默认 false，log() 直接 return
        AppLogger.setEnabled(true)
        AppLogger.clear()
        AppLogger.log("TestTag", "测试日志消息XYZABC")

        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                LogScreen(onBack = {}, onExport = { _, _ -> })
            }
        }
        waitText("测试日志消息XYZABC")
        onNodeWithText("清空").assertExists()
        onNodeWithText("导出").assertExists()
        // 条目头部标签（INFO 级别 tag 为 "INFO"）
        onAllNodesWithText("[INFO/TestTag]")[0].assertExists()
    }

    @Test
    fun 导出按钮回调文件名与内容() = runUltronUiTest {
        ensureKoin()
        AppLogger.setEnabled(true)
        AppLogger.clear()
        AppLogger.log("TestTag", "导出内容日志QRS")

        var exportedName: String? = null
        var exportedContent: String? = null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                LogScreen(
                    onBack = {},
                    onExport = { name, content ->
                        exportedName = name
                        exportedContent = content
                    },
                )
            }
        }
        waitText("导出内容日志QRS")
        onNodeWithText("导出").performSemanticsAction(SemanticsActions.OnClick)
        waitUntil(timeoutMillis = 3_000) { exportedName != null }
        assertTrue(exportedName!!.startsWith("Chrnova-"))
        assertTrue(exportedName!!.endsWith(".log"))
        assertTrue(exportedContent!!.contains("导出内容日志QRS"))
    }
}
