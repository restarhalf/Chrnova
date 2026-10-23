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
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.PEFreeApplyAtt
import restarhalf.stellar.schedule.domain.model.PEFreeApplyDetailResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyItem
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.usecase.PEFreeApplyUseCase
import restarhalf.stellar.schedule.ui.viewmodel.PEFreeApplyViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class PEFreeApplyDetailScreenUiTest {

    private val peGateway = mock<PEGateway>(MockMode.autofill)
    private val peAuth = mock<PEAuthPort>(MockMode.autofill)
    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    private fun makeViewModel(): PEFreeApplyViewModel {
        every { peAuth.observeToken() } returns MutableStateFlow("tok")
        return PEFreeApplyViewModel(
            useCase = PEFreeApplyUseCase(peGateway, peAuthWorkflow),
            peAuth = peAuth,
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染申请信息各字段() = runUltronUiTest {
        everySuspend { peGateway.getFreeApplyDetail(any()) } returns PEFreeApplyDetailResponse(
            status = "1",
            data = PEFreeApplyItem(
                applyId = "ap1",
                schoolYear = "2025-2026",
                freeApplyType = "free",
                applyStatus = "2",
                stuName = "张三",
                stdNumber = "2023001",
                schoolGrade = "2023",
                applyTime = "2026-03-01 09:00",
                remark = "已通过审核",
                attList = emptyList(),
            ),
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyDetailScreen(vm = makeViewModel(), applyId = "ap1", onImageClick = {}, onBack = {})
            }
        }
        waitText("申请信息")

        onNodeWithText("申请详情").assertIsDisplayed()
        onNodeWithText("学年").assertIsDisplayed()
        onNodeWithText("2025-2026").assertIsDisplayed()
        onNodeWithText("申请类型").assertIsDisplayed()
        onNodeWithText("免测").assertIsDisplayed()
        onNodeWithText("状态").assertIsDisplayed()
        onNodeWithText("通过").assertIsDisplayed()
        onNodeWithText("姓名").assertIsDisplayed()
        onNodeWithText("张三").assertIsDisplayed()
        onNodeWithText("学号").assertIsDisplayed()
        onNodeWithText("2023001").assertIsDisplayed()
        onNodeWithText("年级").assertIsDisplayed()
        onNodeWithText("2023").assertIsDisplayed()
        onNodeWithText("申请时间").assertIsDisplayed()
        onNodeWithText("备注").assertIsDisplayed()
        onNodeWithText("已通过审核").assertIsDisplayed()
    }

    @Test
    fun 无附件时显示暂无附件() = runUltronUiTest {
        everySuspend { peGateway.getFreeApplyDetail(any()) } returns PEFreeApplyDetailResponse(
            data = PEFreeApplyItem(applyId = "ap1", schoolYear = "2025-2026", attList = emptyList()),
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyDetailScreen(vm = makeViewModel(), applyId = "ap1", onImageClick = {}, onBack = {})
            }
        }
        waitText("证明材料")
        onNodeWithText("暂无附件").assertIsDisplayed()
    }

    @Test
    fun 图片附件显示加载入口并下载后可查看() = runUltronUiTest {
        everySuspend { peGateway.getFreeApplyDetail(any()) } returns PEFreeApplyDetailResponse(
            data = PEFreeApplyItem(
                applyId = "ap1",
                schoolYear = "2025-2026",
                attList = listOf(PEFreeApplyAtt(attId = "att1", fileName = "证明.jpg")),
            ),
        )
        every { peGateway.peFileDownloadUrl("att1") } returns "http://example.com/att1"
        everySuspend { peGateway.downloadPeFile("att1") } returns byteArrayOf(1, 2, 3)
        var openedUrl: String? = null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyDetailScreen(
                    vm = makeViewModel(),
                    applyId = "ap1",
                    onImageClick = { openedUrl = it },
                    onBack = {},
                )
            }
        }
        waitText("证明.jpg")

        onNodeWithText("加载").performClick()
        waitText("查看")

        onNodeWithText("证明.jpg").performClick()
        waitUntil(timeoutMillis = 3_000) { openedUrl == "http://example.com/att1" }
    }

    @Test
    fun 详情加载失败显示错误提示() = runUltronUiTest {
        everySuspend { peGateway.getFreeApplyDetail(any()) } returns PEFreeApplyDetailResponse(
            status = "0",
            message = "免测申请不存在",
            data = null,
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyDetailScreen(vm = makeViewModel(), applyId = "ap1", onImageClick = {}, onBack = {})
            }
        }
        waitText("未找到申请详情")
        onNodeWithText("未找到申请详情").assertIsDisplayed()
    }

    @Test
    fun 返回按钮回调() = runUltronUiTest {
        everySuspend { peGateway.getFreeApplyDetail(any()) } returns PEFreeApplyDetailResponse(
            data = PEFreeApplyItem(applyId = "ap1", schoolYear = "2025-2026"),
        )
        var backCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyDetailScreen(
                    vm = makeViewModel(),
                    applyId = "ap1",
                    onImageClick = {},
                    onBack = { backCalls++ },
                )
            }
        }
        waitText("申请信息")
        onNodeWithContentDescription("返回").performClick()
        waitUntil(timeoutMillis = 2_000) { backCalls == 1 }
    }
}
