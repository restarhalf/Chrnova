@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.PECodeItem
import restarhalf.stellar.schedule.domain.model.PEFreeApplyItem
import restarhalf.stellar.schedule.domain.model.PEFreeApplyListResponse
import restarhalf.stellar.schedule.domain.model.PEFreeSchoolYearData
import restarhalf.stellar.schedule.domain.model.PEFreeSchoolYearResponse
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.usecase.PEFreeApplyUseCase
import restarhalf.stellar.schedule.ui.viewmodel.PEFreeApplyViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class PEFreeApplyScreenUiTest {

    private val peGateway = mock<PEGateway>(MockMode.autofill)
    private val peAuth = mock<PEAuthPort>(MockMode.autofill)
    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    private val tokenFlow = MutableStateFlow("")

    private fun makeViewModel(): PEFreeApplyViewModel {
        every { peAuth.observeToken() } returns tokenFlow
        return PEFreeApplyViewModel(
            useCase = PEFreeApplyUseCase(peGateway, peAuthWorkflow),
            peAuth = peAuth,
        )
    }

    private fun stubLoggedInData(items: List<PEFreeApplyItem>) {
        everySuspend { peGateway.getFreeApplyList() } returns PEFreeApplyListResponse(
            status = "1",
            data = items,
        )
        everySuspend { peGateway.getFreeSchoolYears() } returns PEFreeSchoolYearResponse(
            status = "1",
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
    fun 未登录显示登录入口且无申请按钮() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(vm = makeViewModel(), onLogin = {}, onApply = {}, onDetail = {}, onBack = {})
            }
        }
        waitText("免测申请")
        onNodeWithText("登录").assertIsDisplayed()
        onNodeWithText("登录后可查看免测申请").assertIsDisplayed()
        assertEquals(0, onAllNodesWithText("我要申请").fetchSemanticsNodes().size)
    }

    @Test
    fun 未登录点击登录回调() = runUltronUiTest {
        var loginCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(
                    vm = makeViewModel(),
                    onLogin = { loginCalls++ },
                    onApply = {},
                    onDetail = {},
                    onBack = {},
                )
            }
        }
        waitText("登录后可查看免测申请")
        onNodeWithText("登录").performClick()
        waitUntil(timeoutMillis = 2_000) { loginCalls == 1 }
    }

    @Test
    fun 已登录渲染申请记录与状态() = runUltronUiTest {
        tokenFlow.value = "tok"
        stubLoggedInData(
            listOf(
                PEFreeApplyItem(
                    applyId = "ap1",
                    schoolYear = "2025-2026",
                    freeApplyType = "free",
                    applyStatus = "2",
                    applyTime = "2026-03-01 09:00",
                ),
            ),
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(vm = makeViewModel(), onLogin = {}, onApply = {}, onDetail = {}, onBack = {})
            }
        }
        waitText("2025-2026【免测】")

        onNodeWithText("2025-2026【免测】").assertIsDisplayed()
        onNodeWithText("2026-03-01 09:00").assertIsDisplayed()
        onNodeWithText("通过").assertIsDisplayed()
        onNodeWithText("我要申请").assertIsDisplayed()
    }

    @Test
    fun 重复申请记录不再崩溃且逐条渲染() = runUltronUiTest {
        tokenFlow.value = "tok"

        stubLoggedInData(
            listOf(
                PEFreeApplyItem(
                    applyId = "ap1",
                    schoolYear = "2025-2026",
                    freeApplyType = "free",
                    applyStatus = "2",
                    applyTime = "2026-03-01 09:00",
                ),
                PEFreeApplyItem(
                    applyId = "ap1",
                    schoolYear = "2025-2026",
                    freeApplyType = "free",
                    applyStatus = "2",
                    applyTime = "2026-03-01 09:00",
                ),
            ),
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(vm = makeViewModel(), onLogin = {}, onApply = {}, onDetail = {}, onBack = {})
            }
        }
        waitText("2025-2026【免测】")

        assertEquals(1, onAllNodesWithText("2025-2026【免测】").fetchSemanticsNodes().size)
    }

    @Test
    fun 记录从空变为非空时不崩溃() = runUltronUiTest {
        tokenFlow.value = "tok"
        val listFlow = MutableStateFlow<List<PEFreeApplyItem>>(emptyList())
        everySuspend { peGateway.getFreeApplyList() } calls {
            PEFreeApplyListResponse(status = "1", data = listFlow.value)
        }
        everySuspend { peGateway.getFreeSchoolYears() } returns PEFreeSchoolYearResponse(
            status = "1",
            data = PEFreeSchoolYearData(nowSchoolYear = "2025-2026", dataList = emptyList()),
        )
        val vm = makeViewModel()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(vm = vm, onLogin = {}, onApply = {}, onDetail = {}, onBack = {})
            }
        }
        waitText("暂无申请记录")

        listFlow.value = listOf(
            PEFreeApplyItem(applyId = "ap1", schoolYear = "2025-2026", freeApplyType = "free", applyStatus = "2"),
        )
        vm.refresh()
        waitText("2025-2026【免测】")
        onNodeWithText("2025-2026【免测】").assertIsDisplayed()
    }

    @Test
    fun 已登录无记录显示空态() = runUltronUiTest {
        tokenFlow.value = "tok"
        stubLoggedInData(emptyList())
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(vm = makeViewModel(), onLogin = {}, onApply = {}, onDetail = {}, onBack = {})
            }
        }
        waitText("暂无申请记录")
        onNodeWithText("暂无申请记录").assertIsDisplayed()
    }

    @Test
    fun 点击申请记录回调onDetail() = runUltronUiTest {
        tokenFlow.value = "tok"
        stubLoggedInData(
            listOf(
                PEFreeApplyItem(
                    applyId = "ap1",
                    schoolYear = "2025-2026",
                    freeApplyType = "free",
                    applyStatus = "1",
                ),
            ),
        )
        var detailId: String? = null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(
                    vm = makeViewModel(),
                    onLogin = {},
                    onApply = {},
                    onDetail = { detailId = it },
                    onBack = {},
                )
            }
        }
        waitText("2025-2026【免测】")
        onNodeWithText("2025-2026【免测】").performClick()
        waitUntil(timeoutMillis = 3_000) { detailId == "ap1" }
    }

    @Test
    fun 点击我要申请回调onApply() = runUltronUiTest {
        tokenFlow.value = "tok"
        stubLoggedInData(emptyList())
        var applyCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(
                    vm = makeViewModel(),
                    onLogin = {},
                    onApply = { applyCalls++ },
                    onDetail = {},
                    onBack = {},
                )
            }
        }
        waitText("我要申请")
        onNodeWithText("我要申请").performClick()
        waitUntil(timeoutMillis = 2_000) { applyCalls == 1 }
    }

    @Test
    fun 返回按钮回调() = runUltronUiTest {
        var backCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEFreeApplyScreen(
                    vm = makeViewModel(),
                    onLogin = {},
                    onApply = {},
                    onDetail = {},
                    onBack = { backCalls++ },
                )
            }
        }
        waitText("免测申请")
        onNodeWithContentDescription("返回").performClick()
        waitUntil(timeoutMillis = 2_000) { backCalls == 1 }
    }
}
