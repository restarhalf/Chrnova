@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.papers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.usecase.VerifyGitHubStarUseCase
import restarhalf.stellar.schedule.papers.PickedAttachment
import restarhalf.stellar.schedule.ui.viewmodel.PapersViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class PapersUploadScreenUiTest {

    private val papersPort = mock<PapersPort>(MockMode.autofill)
    private val settings = mock<DeviceSettingsPort>(MockMode.autofill)

    private fun createVm() = PapersViewModel(
        papersPort = papersPort,
        settings = settings,
        verifyGitHubStar = VerifyGitHubStarUseCase(papersPort, settings),
    )

    private val pickedFile = PickedAttachment(
        bytes = byteArrayOf(1, 2, 3),
        name = "高数期末.pdf",
        mime = "application/pdf",
    )

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** 分组标题与卡片标题同为「选择文件」，只有卡片带点击动作。 */
    private fun androidx.compose.ui.test.ComposeUiTest.clickFilePicker() {
        onNode(hasText("选择文件") and hasClickAction()).performClick()
    }

    /** 表单里两个 TextField 自上而下是「标题」「文件夹」，索引 1 即文件夹。 */
    private fun androidx.compose.ui.test.ComposeUiTest.fillFolder(value: String) {
        onAllNodes(hasSetTextAction())[1].performTextReplacement(value)
    }

    @Test
    fun 渲染表单且未填完时上传按钮禁用() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersUploadScreen(vm = createVm(), onBack = {}, onResult = {})
            }
        }
        waitText("上传试卷")
        onNodeWithText("试卷信息").assertIsDisplayed()
        onNodeWithText("标题").assertIsDisplayed()
        onNodeWithText("文件夹").assertIsDisplayed()
        onAllNodesWithText("选择文件")[0].assertIsDisplayed()
        onNodeWithText("上传").assertIsNotEnabled()
    }

    @Test
    fun 选择文件后回填文件名与标题() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersUploadScreen(
                    vm = createVm(),
                    onBack = {},
                    onResult = {},
                    pdfFilePickerHost = { _, onPicked -> onPicked(listOf(pickedFile)) },
                )
            }
        }
        waitText("选择文件")
        clickFilePicker()

        waitText("已选择文件")
        onNodeWithText("已选择文件").assertIsDisplayed()
        waitText("高数期末.pdf")
        onNodeWithText("高数期末.pdf").assertIsDisplayed()
        // 标题自动取文件名去掉扩展名
        waitText("高数期末")
        onAllNodesWithText("高数期末")[0].assertIsDisplayed()
        // 文件夹还空着，按钮仍禁用
        onNodeWithText("上传").assertIsNotEnabled()
    }

    @Test
    fun 表单填完后上传成功并显示提示() = runUltronUiTest {
        everySuspend { papersPort.uploadPaper(any(), any(), any(), any(), any()) } returns
            Paper(id = "pf9", title = "高数期末")
        everySuspend { papersPort.listPapers() } returns emptyList()
        everySuspend { papersPort.getFolders() } returns emptyList()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersUploadScreen(
                    vm = createVm(),
                    onBack = {},
                    onResult = {},
                    pdfFilePickerHost = { _, onPicked -> onPicked(listOf(pickedFile)) },
                )
            }
        }
        waitText("选择文件")
        clickFilePicker()
        waitText("已选择文件")
        fillFolder("高等数学")

        onNodeWithText("上传").performClick()

        waitText("上传成功")
        onNodeWithText("上传成功").assertIsDisplayed()
    }

    @Test
    fun 上传失败显示错误文案() = runUltronUiTest {
        everySuspend { papersPort.uploadPaper(any(), any(), any(), any(), any()) } throws
            IllegalStateException("文件过大")
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersUploadScreen(
                    vm = createVm(),
                    onBack = {},
                    onResult = {},
                    pdfFilePickerHost = { _, onPicked -> onPicked(listOf(pickedFile)) },
                )
            }
        }
        waitText("选择文件")
        clickFilePicker()
        waitText("已选择文件")
        fillFolder("高等数学")

        onNodeWithText("上传").performClick()
        waitText("文件过大")
        onNodeWithText("文件过大").assertIsDisplayed()
    }

    @Test
    fun 返回按钮回调() = runUltronUiTest {
        var backCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PapersUploadScreen(vm = createVm(), onBack = { backCalls++ }, onResult = {})
            }
        }
        waitText("上传试卷")
        onNodeWithContentDescription("返回").performClick()
        waitUntil(timeoutMillis = 2_000) { backCalls == 1 }
        assertEquals(1, backCalls)
    }
}
