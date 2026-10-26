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
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class PapersScreenUiTest {

    private val papersPort = mock<PapersPort>(MockMode.autofill)
    private val settings = mock<DeviceSettingsPort>(MockMode.autofill)

    private val samplePapers = listOf(
        Paper(id = "pf1", title = "高数期末.pdf", folder = "高等数学"),
        Paper(id = "pf2", title = "高数期中.pdf", folder = "高等数学"),
        Paper(id = "p_root", title = "线代期末.pdf"),
    )

    private fun createVm() = PapersViewModel(papersPort, settings, VerifyGitHubStarUseCase(papersPort, settings))

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

    @Test
    fun 详情页渲染试卷信息并透传代理下载链接() = runUltronUiTest {
        everySuspend { papersPort.getPaper("p1") } returns Paper(id = "p1", title = "高数期末.pdf", folder = "高等数学")
        everySuspend { papersPort.downloadPaper("p1") } returns "https://chrnova.paper.example/file/p1"
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

        onNodeWithText("下载").performSemanticsAction(SemanticsActions.OnClick)
        waitUntil(timeoutMillis = 3_000) { downloads.isNotEmpty() }
        assertEquals("https://chrnova.paper.example/file/p1", downloads[0].first)
        assertEquals("高数期末.pdf", downloads[0].second)
    }

}
