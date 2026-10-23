@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
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
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.PEAppointmentActionResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentDetail
import restarhalf.stellar.schedule.domain.model.PEAppointmentDetailResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentItem
import restarhalf.stellar.schedule.domain.model.PEAppointmentListData
import restarhalf.stellar.schedule.domain.model.PEAppointmentListResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentTimeSlot
import restarhalf.stellar.schedule.domain.model.PEAppointmentTimesResponse
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.PEGateway
import restarhalf.stellar.schedule.domain.usecase.PEAppointmentDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PEAppointmentListUseCase
import restarhalf.stellar.schedule.domain.usecase.PECancelAppointmentUseCase
import restarhalf.stellar.schedule.domain.usecase.PEEnterAppointmentUseCase
import restarhalf.stellar.schedule.ui.viewmodel.PEAppointmentViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class PEAppointmentScreenUiTest {

    private val peGateway = mock<PEGateway>(MockMode.autofill)
    private val peAuth = mock<PEAuthPort>(MockMode.autofill)
    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    private val tokenFlow = MutableStateFlow("")

    private val myItem = PEAppointmentItem(
        appointmentId = "a1",
        temporaryId = "t1",
        appointmentName = "2026 春季体测",
        appointmentStatus = "1",
        appointmentDate = "2026-04-01",
        appointmentTimes = "09:00~10:00",
        enterStartTime = "2026-03-01 08:00:00",
        enterEndTime = "2099-12-31 23:59:59",
        crtTime = "2026-03-02 10:00:00",
        alreadyQuota = 3,
        timeQuota = 50,
    )

    private val availableItem = PEAppointmentItem(
        appointmentId = "a2",
        appointmentName = "2026 秋季体测",
        appointmentStatus = "3",
        enterStartTime = "2026-09-01 08:00:00",
        enterEndTime = "2026-09-30 23:59:59",
        alreadyQuota = 0,
        timeQuota = 100,
    )

    private fun makeViewModel(): PEAppointmentViewModel {
        every { peAuth.observeToken() } returns tokenFlow
        every { peAuth.observeProfile() } returns MutableStateFlow(PEAuthProfile())
        return PEAppointmentViewModel(
            appointmentListUseCase = PEAppointmentListUseCase(peGateway, peAuthWorkflow),
            appointmentDetailUseCase = PEAppointmentDetailUseCase(peGateway, peAuthWorkflow),
            cancelAppointmentUseCase = PECancelAppointmentUseCase(peGateway, peAuthWorkflow),
            enterAppointmentUseCase = PEEnterAppointmentUseCase(peGateway, peAuthWorkflow),
            peAuth = peAuth,
        )
    }

    private fun stubLists() {
        everySuspend {
            peGateway.getAppointments(PEAppointmentViewModel.TYPE_MY, any(), any())
        } returns PEAppointmentListResponse(
            data = PEAppointmentListData(dataList = listOf(myItem), totalRows = "1"),
        )
        everySuspend {
            peGateway.getAppointments(PEAppointmentViewModel.TYPE_AVAILABLE, any(), any())
        } returns PEAppointmentListResponse(
            data = PEAppointmentListData(dataList = listOf(availableItem), totalRows = "1"),
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 未登录显示登录入口() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentScreen(vm = makeViewModel(), onLogin = {}, onBack = {})
            }
        }
        waitText("体测预约")
        onNodeWithText("登录体测系统").assertIsDisplayed()
        onNodeWithText("登录后可查看与管理体测预约").assertIsDisplayed()
        assertEquals(0, onAllNodesWithText("我的预约").fetchSemanticsNodes().size)
    }

    @Test
    fun 未登录点击登录入口回调() = runUltronUiTest {
        var loginCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentScreen(vm = makeViewModel(), onLogin = { loginCalls++ }, onBack = {})
            }
        }
        waitText("登录体测系统")
        onNodeWithText("登录体测系统").performClick()
        waitUntil(timeoutMillis = 2_000) { loginCalls == 1 }
    }

    @Test
    fun 已登录渲染我的预约与状态() = runUltronUiTest {
        tokenFlow.value = "tok"
        stubLists()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentScreen(vm = makeViewModel(), onLogin = {}, onBack = {})
            }
        }
        waitText("2026 春季体测")

        onNodeWithText("我的预约").assertIsDisplayed()
        onNodeWithText("可预约").assertIsDisplayed()
        onNodeWithText("2026 春季体测").assertIsDisplayed()
        onNodeWithText("报名时间").assertIsDisplayed()
        onNodeWithText("2026-03-02 10:00:00").assertIsDisplayed()
        onNodeWithText("预约人数").assertIsDisplayed()
        onNodeWithText("3/50").assertIsDisplayed()
    }

    @Test
    fun 已登录我的预约为空显示空态() = runUltronUiTest {
        tokenFlow.value = "tok"
        everySuspend {
            peGateway.getAppointments(any(), any(), any())
        } returns PEAppointmentListResponse(
            data = PEAppointmentListData(dataList = emptyList(), totalRows = "0"),
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentScreen(vm = makeViewModel(), onLogin = {}, onBack = {})
            }
        }
        waitText("暂无我的预约")
        onNodeWithText("暂无我的预约").assertIsDisplayed()
    }

    @Test
    fun 切换到可预约Tab显示场次并可打开预约弹窗() = runUltronUiTest {
        tokenFlow.value = "tok"
        stubLists()
        everySuspend {
            peGateway.getAppointmentDetail(any(), any())
        } returns PEAppointmentDetailResponse(
            data = PEAppointmentDetail(
                appointmentId = "a2",
                appointmentName = "2026 秋季体测",
                availableDates = listOf("2026-09-10"),
                appointmentContent = "请携带学生证",
            ),
        )
        everySuspend {
            peGateway.getAppointmentTimes(any(), any())
        } returns PEAppointmentTimesResponse(
            dataList = listOf(
                PEAppointmentTimeSlot(
                    timesId = "ts1",
                    appointmentId = "a2",
                    startTime = "09:00",
                    endTime = "10:00",
                    enters = 5,
                    quota = 30,
                ),
            ),
        )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentScreen(vm = makeViewModel(), onLogin = {}, onBack = {})
            }
        }
        waitText("我的预约")
        onNodeWithText("可预约").performClick()

        waitText("2026 秋季体测")
        onNodeWithText("名额").assertIsDisplayed()
        onNodeWithText("0/100").assertIsDisplayed()

        onNodeWithText("预约").performClick()
        waitText("预约体测")
        onNodeWithText("预约体测").assertIsDisplayed()
        waitText("请携带学生证")
        onNodeWithText("请携带学生证").assertIsDisplayed()
        waitText("2026-09-10")
        onNodeWithText("2026-09-10").assertIsDisplayed()
    }

    @Test
    fun 取消预约弹窗确认后提示结果() = runUltronUiTest {
        tokenFlow.value = "tok"
        stubLists()
        everySuspend { peGateway.cancelAppointment(any()) } returns
            PEAppointmentActionResponse(status = "1", message = "取消成功")
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentScreen(vm = makeViewModel(), onLogin = {}, onBack = {})
            }
        }
        waitText("2026 春季体测")

        onNodeWithText("取消预约").performClick()
        waitText("再想想")
        onNodeWithText("再想想").assertIsDisplayed()

        onAllNodesWithText("取消预约").onLast().performClick()

        waitText("取消成功")
        onNodeWithText("取消成功").assertIsDisplayed()
        onNodeWithText("知道了").assertIsDisplayed()
    }

    @Test
    fun 返回按钮回调() = runUltronUiTest {
        var backCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEAppointmentScreen(vm = makeViewModel(), onLogin = {}, onBack = { backCalls++ })
            }
        }
        waitText("体测预约")
        onNodeWithContentDescription("返回").performClick()
        waitUntil(timeoutMillis = 2_000) { backCalls == 1 }
    }
}
