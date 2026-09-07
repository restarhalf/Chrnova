@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
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
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.ui.viewmodel.CourseEditViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 课程编辑页（实验课）Compose UI 测试（Ultron KMP 入口）。
 *
 * VM 配方同 JVM 测试：courseRepository.observeAllCourses + auth.observeProfile。
 * 新建模式（courseId=null, initialSelectedWeek=1）表单预选第 1 周，
 * 点击"确定"经 buildLabCourseToSave → saveLabCourse(insertCourse) → onBack。
 */
class CourseEditScreenUiTest {

    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)

    private val coursesFlow = MutableStateFlow(
        listOf(
            Course(
                name = "大学物理实验",
                location = "实验楼301",
                teacher = "李四",
                dayOfWeek = 3,
                startSection = 1,
                sectionCount = 2,
                weeks = listOf(1, 2, 3),
                color = "#4CAF50",
            ),
        ),
    )
    private val profileFlow = MutableStateFlow(JwxtAuthProfile())

    private fun makeViewModel(): CourseEditViewModel {
        every { courseRepository.observeAllCourses() } returns coursesFlow
        // 屏幕经 observeEditingCourse 收集该流，autofill 对未知 Flow 返回 null 会 NPE
        every { courseRepository.observeCourseById(any()) } returns MutableStateFlow<Course?>(null)
        every { auth.observeProfile() } returns profileFlow
        return CourseEditViewModel(courseRepository, auth)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染表单分组与选择项() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseEditScreen(
                    vm = makeViewModel(),
                    onBack = {},
                    isEdit = false,
                    onEditChanged = {},
                )
            }
        }
        waitText("编辑实验课")
        onNodeWithText("课程名").assertExists()
        onNodeWithText("教室").assertExists()
        onNodeWithText("教师").assertExists()
        onNodeWithText("上课星期").assertExists()
        onNodeWithText("上课时间").assertExists()
        onNodeWithText("上课周数").assertExists()
    }

    @Test
    fun 点击确定保存并回调返回() = runUltronUiTest {
        everySuspend { courseRepository.insertCourse(any()) } returns 1L
        var backed = false
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CourseEditScreen(
                    vm = makeViewModel(),
                    onBack = { backed = true },
                    isEdit = false,
                    onEditChanged = {},
                )
            }
        }
        waitText("课程名")
        runOnIdle { }
        // 顶栏"确定"保存按钮（新建模式默认预选第 1 周，表单可保存）
        onNode(hasContentDescription("确定")).performSemanticsAction(SemanticsActions.OnClick)
        waitUntil(timeoutMillis = 5_000) { backed }
        assertEquals(true, backed)
    }
}
