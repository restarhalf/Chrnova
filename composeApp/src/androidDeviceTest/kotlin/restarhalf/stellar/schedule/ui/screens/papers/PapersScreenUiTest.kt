@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.papers

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 试卷模块 Compose UI 测试（Ultron KMP 入口）
 *
 * PapersViewModel 手动构造（不依赖 Koin）：papersPort / settings 用 mokkery mock，
 * VerifyGitHubStarUseCase 用真实实现桥接两个 mock。
 * 星验证对话框（StarVerificationDialog）基于 androidx Dialog 独立窗口渲染，
 * 主窗口语义树查询不到其内容，故以"门禁行为"（未验证不加载列表）代替对话框文案断言。
 */
class PapersScreenUiTest {

    private val papersPort = mock<PapersPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    private val samplePapers = listOf(
        Paper(id = "pf1", title = "高数期末.pdf", folder = "高等数学"),
        Paper(id = "pf2", title = "高数期中.pdf", folder = "高等数学"),
        Paper(id = "p_root", title = "线代期末.pdf"),
    )

    private fun createVm() = PapersViewModel(papersPort, settings, VerifyGitHubStarUseCase(papersPort, settings))

    /** 已通过星验证 + 固定列表/文件夹数据的常规前置 */
    private fun stubVerified(papers: List<Paper>, folders: List<String>) {
        every { settings.getStarVerified() } returns true
        everySuspend { papersPort.listPapers() } returns papers
        everySuspend { papersPort.getFolders() } returns folders
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // region PapersListScreen

    @Test
    fun 未验证时列表不加载() = runUltronUiTest {
        every { settings.getStarVerified() } returns false
        everySuspend { papersPort.listPapers() } returns samplePapers
        everySuspend { papersPort.getFolders() } returns listOf("高等数学")
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersListScreen(vm = createVm(), onBack = {}, onPaperDetail = {}, onUploadClick = {})
            }
        }
        waitText("试卷共享")
        // 验证门禁行为：isVerified=false 时 LaunchedEffect 不触发 loadFolders/loadPapers。
        // 留出时间窗，若门禁失效（误加载）这里会变为非空。
        Thread.sleep(500)
        assertEquals(0, onAllNodesWithText("高等数学").fetchSemanticsNodes().size)
        assertEquals(0, onAllNodesWithText("线代期末.pdf").fetchSemanticsNodes().size)
    }

    @Test
    fun 已验证渲染标题文件夹与根试卷() = runUltronUiTest {
        stubVerified(samplePapers, listOf("高等数学"))
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersListScreen(vm = createVm(), onBack = {}, onPaperDetail = {}, onUploadClick = {})
            }
        }
        waitText("试卷共享")
        waitText("高等数学")
        onNodeWithText("高等数学").assertIsDisplayed()
        onNodeWithText("2份试卷").assertIsDisplayed()
        onNodeWithText("线代期末.pdf").assertIsDisplayed()
    }

    @Test
    fun 展开文件夹显示试卷并回调详情ID() = runUltronUiTest {
        stubVerified(samplePapers, listOf("高等数学"))
        val detailIds = mutableListOf<String>()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersListScreen(vm = createVm(), onBack = {}, onPaperDetail = { detailIds.add(it) }, onUploadClick = {})
            }
        }
        waitText("高等数学")
        onNodeWithText("高等数学").performClick()
        waitText("高数期末.pdf")
        // 等待展开动画结束再点击
        waitForIdle()
        onNodeWithText("高数期末.pdf").performClick()
        waitUntil(timeoutMillis = 2_000) { detailIds.isNotEmpty() }
        assertEquals("pf1", detailIds[0])
    }

    @Test
    fun 点击根试卷回调详情ID() = runUltronUiTest {
        stubVerified(samplePapers, listOf("高等数学"))
        val detailIds = mutableListOf<String>()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersListScreen(vm = createVm(), onBack = {}, onPaperDetail = { detailIds.add(it) }, onUploadClick = {})
            }
        }
        waitText("线代期末.pdf")
        onNodeWithText("线代期末.pdf").performClick()
        waitUntil(timeoutMillis = 2_000) { detailIds.isNotEmpty() }
        assertEquals("p_root", detailIds[0])
    }

    @Test
    fun 空列表显示暂无试卷() = runUltronUiTest {
        stubVerified(emptyList(), emptyList())
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersListScreen(vm = createVm(), onBack = {}, onPaperDetail = {}, onUploadClick = {})
            }
        }
        waitText("暂无试卷")
        onNodeWithText("暂无试卷").assertIsDisplayed()
    }

    // endregion

    // region PapersDetailScreen

    /**
     * 渲染 + 下载回调合并为一个用例：同类内对 PapersDetailScreen 二次 setContent
     * 在全量回归负载下会触发 Compose 框架重入布局崩溃
     * （"performMeasureAndLayout called during measure layout"），单 setContent 稳定。
     */
    @Test
    fun 详情页渲染试卷信息并透传代理下载链接() = runUltronUiTest {
        everySuspend { papersPort.getPaper("p1") } returns Paper(id = "p1", title = "高数期末.pdf", folder = "高等数学")
        everySuspend { papersPort.downloadPaper("p1") } returns "https://raw.example.com/file.pdf"
        val downloads = mutableListOf<Pair<String, String>>()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersDetailScreen(
                    vm = createVm(),
                    paperId = "p1",
                    onBack = {},
                    onDownload = { url, title -> downloads.add(url to title) },
                )
            }
        }
        waitText("试卷详情")
        waitText("高数期末.pdf")
        onNodeWithText("试卷信息").assertIsDisplayed()
        onNodeWithText("高等数学").assertIsDisplayed()
        onNodeWithText("下载").assertIsDisplayed()
        waitForIdle()
        // 直调语义 OnClick 动作而非触碰注入：规避触碰注入与布局同步的竞态，
        // 同时绕开 edge-to-edge 下导航栏吸收底部点击的问题
        onNodeWithText("下载").performSemanticsAction(SemanticsActions.OnClick)
        waitUntil(timeoutMillis = 3_000) { downloads.isNotEmpty() }
        assertEquals("https://v4.gh-proxy.org/https://raw.example.com/file.pdf", downloads[0].first)
        assertEquals("高数期末.pdf", downloads[0].second)
    }

    // endregion
}
