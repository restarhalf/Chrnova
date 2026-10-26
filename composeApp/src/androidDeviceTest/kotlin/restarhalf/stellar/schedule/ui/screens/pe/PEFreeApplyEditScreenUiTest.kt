@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.PECodeItem
import restarhalf.stellar.schedule.domain.model.PEFreeActionResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyListResponse
import restarhalf.stellar.schedule.domain.model.PEFreeSchoolYearData
import restarhalf.stellar.schedule.domain.model.PEFreeSchoolYearResponse
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.usecase.PEFreeApplyUseCase
import restarhalf.stellar.schedule.papers.PickedAttachment
import restarhalf.stellar.schedule.ui.viewmodel.PEFreeApplyViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class PEFreeApplyEditScreenUiTest {

    private val peGateway = mock<PEGateway>(MockMode.autofill)
    private val peAuth = mock<PEAuthPort>(MockMode.autofill)
    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    private fun makeViewModel(): PEFreeApplyViewModel {
        every { peAuth.observeToken() } returns MutableStateFlow("tok")
        every { peAuth.getStdNumber() } returns "2023001"
        return PEFreeApplyViewModel(
            useCase = PEFreeApplyUseCase(peGateway, peAuthWorkflow),
            peAuth = peAuth,
        )
    }

    private fun stubYears() {
        everySuspend { peGateway.getFreeApplyList() } returns PEFreeApplyListResponse()
        everySuspend { peGateway.getFreeSchoolYears() } returns PEFreeSchoolYearResponse(
            data = PEFreeSchoolYearData(
                nowSchoolYear = "2025-2026",
                dataList = listOf(
                    PECodeItem(code = "2025-2026", label = "2025-2026"),
                    PECodeItem(code = "2024-2025", label = "2024-2025"),
                ),
            ),
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染表单与默认学年() = runUltronUiTest {
        stubYears()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyEditScreen(vm = makeViewModel(), onBack = {}, onSubmitted = {})
            }
        }
        waitText("我要申请")

        onNodeWithText("申请信息").assertIsDisplayed()
        onNodeWithText("申请学年").assertIsDisplayed()
        waitText("2025-2026")
        onNodeWithText("2025-2026").assertIsDisplayed()
        onNodeWithText("申请类型").assertIsDisplayed()
        onNodeWithText("仅支持免测").assertIsDisplayed()
        onNodeWithText("证明材料").assertIsDisplayed()
        onNodeWithText("上传附件").assertIsDisplayed()
        onNodeWithText("提交申请").assertIsDisplayed()
    }

    @Test
    fun 未选类型时提交给出错误提示() = runUltronUiTest {
        stubYears()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyEditScreen(vm = makeViewModel(), onBack = {}, onSubmitted = {})
            }
        }
        waitText("提交申请")
        onNodeWithText("提交申请").performClick()
        waitText("请选择申请类型")
        onNodeWithText("请选择申请类型").assertIsDisplayed()
    }

    @Test
    fun 缺少学号时提交给出错误提示() = runUltronUiTest {
        stubYears()

        val vm = makeViewModel()
        every { peAuth.getStdNumber() } returns null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyEditScreen(vm = vm, onBack = {}, onSubmitted = {})
            }
        }
        waitText("提交申请")
        onNodeWithText("提交申请").performClick()
        waitText("缺少学号，请先完善体测系统资料")
        onNodeWithText("缺少学号，请先完善体测系统资料").assertIsDisplayed()
    }

    @Test
    fun 上传附件后展示文件名并可移除() = runUltronUiTest {
        stubYears()
        everySuspend { peGateway.uploadPeFile(any(), any(), any()) } returns
            PEFreeActionResponse(status = "1", message = "上传成功", attId = "att1")
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyEditScreen(
                    vm = makeViewModel(),
                    onBack = {},
                    onSubmitted = {},
                    pdfFilePickerHost = { _, onPicked ->
                        onPicked(
                            listOf(
                                PickedAttachment(
                                    bytes = byteArrayOf(1, 2, 3),
                                    name = "诊断证明.pdf",
                                    mime = "application/pdf",
                                ),
                            ),
                        )
                    },
                )
            }
        }
        waitText("上传附件")
        onNodeWithText("上传附件").performClick()

        waitText("诊断证明.pdf")
        onNodeWithText("已选 1 个文件").assertIsDisplayed()
        onNodeWithText("PDF").assertIsDisplayed()

        onNodeWithText("移除").performClick()
        waitText("请选择证明材料（≤5M）")
        onNodeWithText("请选择证明材料（≤5M）").assertIsDisplayed()
    }

    @Test
    fun 提交成功后弹窗并回调onSubmitted() = runUltronUiTest {
        stubYears()
        everySuspend { peGateway.uploadPeFile(any(), any(), any()) } returns
            PEFreeActionResponse(status = "1", attId = "att1")
        everySuspend { peGateway.submitFreeApply(any(), any(), any(), any()) } returns
            PEFreeActionResponse(status = "1", message = "申请已提交")
        var submitted = 0
        val vm = makeViewModel()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyEditScreen(
                    vm = vm,
                    onBack = {},
                    onSubmitted = { submitted++ },
                    pdfFilePickerHost = { _, onPicked ->
                        onPicked(
                            listOf(
                                PickedAttachment(
                                    bytes = byteArrayOf(1, 2, 3),
                                    name = "诊断证明.pdf",
                                    mime = "application/pdf",
                                ),
                            ),
                        )
                    },
                )
            }
        }
        waitText("上传附件")
        onNodeWithText("上传附件").performClick()
        waitText("诊断证明.pdf")

        runOnIdle { vm.selectApplyType("free", "免测") }
        waitText("免测")

        onNodeWithText("提交申请").performClick()

        waitText("申请已提交")
        onNodeWithText("知道了").performClick()
        waitUntil(timeoutMillis = 3_000) { submitted == 1 }
    }

    @Test
    fun 取消按钮回调onBack() = runUltronUiTest {
        stubYears()
        var backCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyEditScreen(vm = makeViewModel(), onBack = { backCalls++ }, onSubmitted = {})
            }
        }
        waitText("取消")
        onNodeWithText("取消").performClick()
        waitUntil(timeoutMillis = 2_000) { backCalls == 1 }
        assertEquals(1, backCalls)
    }

    @Test
    fun 提交按钮初始可用且取消可用() = runUltronUiTest {
        stubYears()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyEditScreen(vm = makeViewModel(), onBack = {}, onSubmitted = {})
            }
        }
        waitText("提交申请")

        onNodeWithText("提交申请").assertIsEnabled()
        onNodeWithText("取消").assertIsEnabled()
    }
}
