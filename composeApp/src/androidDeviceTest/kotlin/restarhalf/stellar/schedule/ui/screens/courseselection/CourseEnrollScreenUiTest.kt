@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.courseselection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.JwxtSelectedCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionClassification
import restarhalf.stellar.schedule.domain.model.JwxtSelectionCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionOperResult
import restarhalf.stellar.schedule.domain.model.JwxtSelectionRotation
import restarhalf.stellar.schedule.domain.port.CourseSelectionPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.JwxtSelectionDropResult
import restarhalf.stellar.schedule.domain.port.JwxtSelectionSession
import restarhalf.stellar.schedule.domain.usecase.CourseSelectionUseCase
import restarhalf.stellar.schedule.ui.viewmodel.CourseEnrollViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class CourseEnrollScreenUiTest {

    private val port = mock<CourseSelectionPort>(MockMode.autofill)
    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)

    private val sampleCourse = JwxtSelectionCourse(
        courseName = "高等数学",
        courseNumber = "MATH01",
        courseId = "c1",
        noticeId = "n1",
        kxh = "01",
        classTeacher = "张三",
        classPlace = "教学楼A101",
        classTime = "周一 1-2节",
        period = "64",
        credit = "4",
    )

    private val sampleSelected = JwxtSelectedCourse(
        courseName = "体育",
        noticeId = "n9",
        kxh = "02",
        classTeacher = "李四",
        credit = "1",
        isCanTk = "1",
    )

    private fun makeViewModel() = CourseEnrollViewModel(
        useCase = CourseSelectionUseCase(port, authWorkflow),
    )

    private fun stubSession() {
        everySuspend { authWorkflow.ensureLoggedIn() } returns Unit
        everySuspend { port.fetchRotations() } returns listOf(
            JwxtSelectionRotation(rotationId = "r1", rotationName = "第一轮"),
        )
        everySuspend { port.initSession("r1") } returns JwxtSelectionSession(
            sessionTime = "st-1",
            classifications = listOf(
                JwxtSelectionClassification(classificationCode = "c1", classificationName = "通识"),
            ),
            extraRules = emptyMap(),
        )
        everySuspend { port.fetchCourses(any(), any(), any(), any(), any()) } returns
            listOf(sampleCourse)
        everySuspend { port.fetchSelectedCourses(any()) } returns listOf(sampleSelected)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染标题与选课轮次() = runUltronUiTest {
        stubSession()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseEnrollScreen(vm = makeViewModel(), onBack = {})
            }
        }
        waitText("可选")
        onNodeWithText("可选").assertIsDisplayed()
        // 进入轮次后会拉取已选课程，标签计数从 0 变 1
        waitText("已选(1)")
        onNodeWithText("已选(1)").assertIsDisplayed()
        onAllNodesWithText("选课轮次")[0].assertIsDisplayed()
    }

    @Test
    fun 自动进入轮次后显示分类与课程() = runUltronUiTest {
        stubSession()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseEnrollScreen(vm = makeViewModel(), onBack = {})
            }
        }
        waitText("课程分类")

        onNodeWithText("通识").assertIsDisplayed()
        onNodeWithText("高等数学").assertIsDisplayed()
        onNodeWithText("班次 01 · 张三").assertIsDisplayed()
        onNodeWithText("学分 4").assertIsDisplayed()
    }

    @Test
    fun 选课成功后显示成功提示() = runUltronUiTest {
        stubSession()
        everySuspend {
            port.submitSelection(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns JwxtSelectionOperResult.Success(message = "选课成功")
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseEnrollScreen(vm = makeViewModel(), onBack = {})
            }
        }
        waitText("高等数学")

        onNode(hasText("选课") and hasClickAction()).performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("选课成功：高等数学", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 切换到已选Tab显示已选课程并弹出退课确认() = runUltronUiTest {
        stubSession()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseEnrollScreen(vm = makeViewModel(), onBack = {})
            }
        }
        waitText("已选(1)")
        onNodeWithText("已选(1)").performClick()

        waitText("已选课程")
        onNodeWithText("体育").assertIsDisplayed()
        onNodeWithText("退课").performClick()

        waitText("确认退课")
        onAllNodesWithText("确定要退选「体育」", substring = true)[0].assertIsDisplayed()
    }

    @Test
    fun 确认退课后提示退课成功() = runUltronUiTest {
        stubSession()
        everySuspend { port.dropSelection(any(), any(), any(), any()) } returns
            JwxtSelectionDropResult(success = true, message = "ok")
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseEnrollScreen(vm = makeViewModel(), onBack = {})
            }
        }
        waitText("已选(1)")
        onNodeWithText("已选(1)").performClick()
        waitText("体育")

        onNodeWithText("退课").performClick()
        waitText("确认退课")

        // 弹窗内的「退课」与卡片上的「退课」同名，弹窗后加入树，取最后一个
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("退课").fetchSemanticsNodes().size >= 2
        }
        onAllNodesWithText("退课").onLast().performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("退课成功：体育").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, onAllNodesWithText("确认退课").fetchSemanticsNodes().size)
    }

    @Test
    fun 返回按钮回调() = runUltronUiTest {
        stubSession()
        var backCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseEnrollScreen(vm = makeViewModel(), onBack = { backCalls++ })
            }
        }
        waitText("可选")
        onNodeWithContentDescription("返回").performClick()
        waitUntil(timeoutMillis = 2_000) { backCalls == 1 }
    }
}
