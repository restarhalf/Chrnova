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
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.port.CourseSelectionPort
import restarhalf.stellar.schedule.domain.model.JwxtSelectedCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionClassification
import restarhalf.stellar.schedule.domain.model.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.CourseSelectionServicePort
import restarhalf.stellar.schedule.domain.port.JwxtSelectionSession
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.ServiceLogEntry
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import restarhalf.stellar.schedule.ui.viewmodel.CourseSelectionViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class CourseSelectionScreenUiTest {

    private val port = mock<CourseSelectionPort>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val servicePort = mock<CourseSelectionServicePort>(MockMode.autofill)

    private val runningFlow = MutableStateFlow(false)
    private val latestLogFlow = MutableStateFlow(ServiceLogEntry(""))

    private fun makeViewModel(): CourseSelectionViewModel {
        every { servicePort.running } returns runningFlow
        every { servicePort.latestLog } returns latestLogFlow
        every { servicePort.isSupported } returns false
        return CourseSelectionViewModel(
            useCase = CourseSelectionUseCase(port, authWorkflow),
            servicePort = servicePort,
        )
    }

    private fun stubFullSession() {
        everySuspend { port.fetchRotations() } returns listOf(
            JwxtSelectionRotation(rotationId = "r1", rotationName = "第一轮"),
            JwxtSelectionRotation(rotationId = "r2", rotationName = "第二轮"),
        )
        everySuspend { port.initSession("r1") } returns JwxtSelectionSession(
            sessionTime = "st-1",
            classifications = listOf(
                JwxtSelectionClassification(classificationCode = "c1", classificationName = "通识"),
                JwxtSelectionClassification(classificationCode = "c2", classificationName = "体育"),
            ),
            extraRules = emptyMap(),
        )
        everySuspend { port.fetchSelectedCourses(any()) } returns listOf(
            JwxtSelectedCourse(courseName = "体育", noticeId = "n9"),
            JwxtSelectedCourse(courseName = "大学英语", noticeId = "n8"),
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

        waitText("选课分类")

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

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("已选课程（可退课）").fetchSemanticsNodes().isNotEmpty()
        }

        onAllNodesWithText("体育")[0].assertIsDisplayed()
        onAllNodesWithText("大学英语")[0].assertIsDisplayed()
    }
}
