@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

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
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import restarhalf.stellar.schedule.domain.usecase.CalculateElectiveCreditsUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchSemesterIdsUseCase
import restarhalf.stellar.schedule.ui.viewmodel.ElectiveCreditViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 选修课学分统计页 Compose UI 测试（Ultron KMP 入口）。
 *
 * VM 配方同 JVM 测试：FetchSemesterIdsUseCase / CalculateElectiveCreditsUseCase
 * 为 final class，用真实实现 + mock 端口（AcademicPort / SettingsPort / GradeRepository）。
 * 成绩详情弹窗 GradeDetailsDialog 为 WindowDialog 独立窗口，主窗口语义树不可见，
 * 只断言展开入口与课程列表渲染。
 */
class ElectiveCreditScreenUiTest {

    private val authWorkflow = mock<JwxtAuthWorkflowPort>(MockMode.autofill)
    private val academic = mock<AcademicPort>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val gradeRepository = mock<GradeRepository>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)

    private fun stubSuccess() {
        everySuspend { academic.fetchCurrentTermId() } returns "2026-1"
        everySuspend { academic.fetchSemesterIds() } returns listOf("2026-1")
        everySuspend { academic.fetchGradeReport(any()) } returns
            TermGradeReport(
                achievements = listOf(
                    GradeCourse(
                        courseCode = "X2B002",
                        courseName = "影视鉴赏",
                        score = "90",
                        credit = 1.5,
                        gradePoint = 4.0,
                        semester = "2026-1",
                    ),
                ),
            )
        everySuspend { academic.fetchGuidanceTeachingCourses(any(), any(), any()) } returns emptyList()
        every { settings.observeCachedSemesterIds() } returns flowOf(emptyList())
    }

    private fun makeViewModel() = ElectiveCreditViewModel(
        authWorkflow = authWorkflow,
        academic = academic,
        auth = auth,
        gradeRepository = gradeRepository,
        fetchSemesterIds = FetchSemesterIdsUseCase(authWorkflow, academic, settings),
        calculateElectiveCredits = CalculateElectiveCreditsUseCase(),
    )

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 10_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染学分分类卡片() = runUltronUiTest {
        stubSuccess()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ElectiveCreditScreen(
                    vm = makeViewModel(),
                    onBack = {},
                )
            }
        }
        waitText("选修课学分统计")
        waitText("X2")
        // 类别名称、课程数与学分汇总（90 分及格课程计入 X2，1.5 学分）
        onNodeWithText("X2艺术鉴赏与审美体验").assertExists()
        onNodeWithText("1门课程").assertExists()
        onNodeWithText("1.5").assertExists()
    }

    @Test
    fun 展开类别显示课程列表() = runUltronUiTest {
        stubSuccess()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                ElectiveCreditScreen(
                    vm = makeViewModel(),
                    onBack = {},
                )
            }
        }
        waitText("X2艺术鉴赏与审美体验")
        // 点击类别头部展开课程列表
        onNodeWithText("X2艺术鉴赏与审美体验").performSemanticsAction(SemanticsActions.OnClick)
        // 展开后 GradeItemCard 渲染课程名与成绩分数
        waitText("影视鉴赏")
        onNodeWithText("90").assertExists()
    }
}
