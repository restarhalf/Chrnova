@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.usecase.DeleteExaminationUseCase
import restarhalf.stellar.schedule.domain.usecase.SaveExaminationUseCase
import restarhalf.stellar.schedule.ui.viewmodel.ExamEditViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 考试编辑页 Compose UI 测试（Ultron KMP 入口）。
 *
 * VM 配方同 JVM 测试（courseRepository/examinationRepository/auth/settings/academic）。
 * 新建模式仅渲染表单分组；日期/时间选择器与删除确认均为弹层/独立窗口，只断言门禁字段。
 */
class ExamEditScreenUiTest {

    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)

    private val coursesFlow = MutableStateFlow(
        listOf(
            Course(
                name = "高等数学",
                location = "教学楼101",
                teacher = "张三",
                dayOfWeek = 1,
                startSection = 1,
                sectionCount = 2,
                weeks = listOf(1, 2, 3),
                color = "#FF5722",
            ),
        ),
    )
    private val profileFlow = MutableStateFlow(JwxtAuthProfile())
    private val selectedTermFlow = MutableStateFlow("2025-2026-1")
    private val currentTermFlow = MutableStateFlow("2025-2026-1")

    private fun makeViewModel(): ExamEditViewModel {
        every { courseRepository.observeAllCourses() } returns coursesFlow
        // 屏幕经 observeEditingExamination 收集该流，autofill 对未知 Flow 返回 null 会 NPE
        every { examinationRepository.observeExaminationById(any()) } returns
            MutableStateFlow<Examination?>(null)
        every { settings.observeSelectedTerm() } returns selectedTermFlow
        every { settings.observeCurrentTermId() } returns currentTermFlow
        every { auth.observeProfile() } returns profileFlow
        return ExamEditViewModel(
            courseRepository = courseRepository,
            examinationRepository = examinationRepository,
            auth = auth,
            settings = settings,
            academic = academic,
            saveExaminationUseCase = SaveExaminationUseCase(examinationRepository),
            deleteExaminationUseCase = DeleteExaminationUseCase(examinationRepository),
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染考试编辑表单分组() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ExamEditScreen(
                    vm = makeViewModel(),
                    onBack = {},
                    isEdit = false,
                    onEditChanged = {},
                )
            }
        }
        waitText("编辑考试")
        onNodeWithText("课程名").assertExists()
        onNodeWithText("考试日期").assertExists()
        onNodeWithText("考试时间").assertExists()
        onNodeWithText("考试地点").assertExists()
        onNodeWithText("座位号").assertExists()
        onNodeWithText("备注").assertExists()
    }
}
