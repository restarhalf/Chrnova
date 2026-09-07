@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.courseselection

import androidx.compose.ui.test.assertIsDisplayed
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
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.data.remote.JwxtGateway
import restarhalf.stellar.schedule.data.remote.JwxtSelectionResponse
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ServiceLogEntry
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 自动抢课模块 Compose UI 测试（Ultron KMP 入口）
 *
 * VM 构造同 JVM 测试配方：CourseSelectionUseCase 真实实例 + mock JwxtGateway，
 * session 走真实链路（selectRotation → initSelectionSession 成功响应）。
 * 屏幕 LaunchedEffect 自动 loadRotations 并选中首轮，stub 全链路后页面自动进入会话。
 */
class CourseSelectionScreenUiTest {

    private val gateway = mock<JwxtGateway>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val servicePort = mock<CourseSelectionServicePort>(MockMode.autofill)

    private val runningFlow = MutableStateFlow(false)
    private val latestLogFlow = MutableStateFlow(ServiceLogEntry(""))

    private fun okResponse(dataJson: String? = null) = JwxtSelectionResponse(
        errorCode = "success",
        data = dataJson?.let { Json.parseToJsonElement(it) },
    )

    private fun makeViewModel(): CourseSelectionViewModel {
        every { servicePort.running } returns runningFlow
        every { servicePort.latestLog } returns latestLogFlow
        every { servicePort.isSupported } returns false
        return CourseSelectionViewModel(
            useCase = CourseSelectionUseCase(gateway, authWorkflow),
            servicePort = servicePort,
        )
    }

    /** stub 轮次加载 + 首轮会话初始化 + 已选课程查询（页面自动走完全链路） */
    private fun stubFullSession() {
        everySuspend { gateway.fetchSelectionRotations(any()) } returns okResponse(
            """[{"rotationid":"r1","rotationname":"第一轮"},{"rotationid":"r2","rotationname":"第二轮"}]""",
        )
        everySuspend { gateway.initSelectionSession("r1") } returns okResponse(
            """{"sessionTime":"st-1","classificationList":[
                {"classificationCode":"c1","classificationName":"通识"},
                {"classificationCode":"c2","classificationName":"体育"}]}""",
        )
        everySuspend { gateway.fetchSelectedCourses(any()) } returns okResponse(
            """[{"courseName":"体育","noticeId":"n9"},{"courseName":"大学英语","noticeId":"n8"}]""",
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染标题Tab与底部操作栏() = runUltronUiTest {
        stubFullSession()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseSelectionScreen(vm = makeViewModel(), onBack = {})
            }
        }
        waitText("自动抢课")
        onNodeWithText("自动抢课").assertIsDisplayed()
        onNodeWithText("选课").assertIsDisplayed()
        onNodeWithText("目标(0)").assertIsDisplayed()
        // SmallTitle 与 SuperDropdown title 重复 → 取首个节点
        onAllNodesWithText("选课轮次")[0].assertIsDisplayed()
        onNodeWithText("前台抢课").assertIsDisplayed()
    }

    @Test
    fun 自动进入会话后显示分类() = runUltronUiTest {
        stubFullSession()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseSelectionScreen(vm = makeViewModel(), onBack = {})
            }
        }
        // loadRotations 自动选中首轮 → initSession → 自动选第一个分类"通识"
        waitText("选课分类")
        // 分类下拉列表在弹层中不进主语义树，仅选中项 summary 可见
        onNodeWithText("通识").assertIsDisplayed()
    }

    @Test
    fun 切换到已选Tab显示已选课程() = runUltronUiTest {
        stubFullSession()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseSelectionScreen(vm = makeViewModel(), onBack = {})
            }
        }
        waitText("自动抢课")
        onNodeWithText("已选(2)").performClick()
        // 翻页后"已选"页组合出现
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("已选课程（可退课）").fetchSemanticsNodes().isNotEmpty()
        }
        // 已选列表按行渲染课程名（体育/大学英语 各一行）
        onAllNodesWithText("体育")[0].assertIsDisplayed()
        onAllNodesWithText("大学英语")[0].assertIsDisplayed()
    }
}
