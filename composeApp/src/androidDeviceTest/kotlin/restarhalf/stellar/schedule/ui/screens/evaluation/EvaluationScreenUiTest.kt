@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.evaluation

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
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.domain.model.EvaluationPage
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.port.CourseEvaluationPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.UserProfilePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController


class EvaluationScreenUiTest {

    private val port = mock<CourseEvaluationPort>(MockMode.autofill)
    private val courseRepository = mock<CourseRepository>(MockMode.autofill)
    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val userProfile = mock<UserProfilePort>(MockMode.autofill)

    private val profileFlow = MutableStateFlow(JwxtAuthProfile())

    private val sampleSummary = CourseEvaluationSummary(
        courseName = "高等数学",
        teacher = "张三",
        avgRating = 4.5,
        evalCount = 12,
        latestAt = 1_700_000_000,
    )

    private val sampleEvaluation = Evaluation(
        id = "e1",
        courseName = "高等数学",
        teacher = "张三",
        rating = 4,
        content = "讲得不错，作业量适中",
        author = "小明",
        userHash = "hash_x",
        likes = 3,
    )

    
    private fun createVm(courses: List<Course> = emptyList()): CourseEvaluationViewModel {
        every { userProfile.getUserNickname() } returns null
        every { auth.observeProfile() } returns profileFlow
        everySuspend { courseRepository.getAllCoursesAcrossSemesters() } returns courses
        return CourseEvaluationViewModel(port, courseRepository, auth, userProfile)
    }

    private fun stubListData() {
        everySuspend { port.listCourseSummaries() } returns listOf(sampleSummary)
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns
            EvaluationPage(items = listOf(sampleEvaluation), total = 1)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }



    @Test
    fun 列表渲染课程卡片与统计() = runUltronUiTest {
        stubListData()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationListScreen(
                    vm = createVm(),
                    onBack = {},
                    onCourseClick = { _, _ -> },
                    onEvaluationDetail = {},
                    onSubmitClick = {},
                )
            }
        }
        waitText("课程评价")
        waitText("高等数学")
        onNodeWithText("高等数学").assertIsDisplayed()
        onNodeWithText("张三").assertIsDisplayed()
        onNodeWithText("平均分").assertIsDisplayed()
        onNodeWithText("条评价").assertIsDisplayed()
    }

    @Test
    fun 点击课程卡片回调课程与教师() = runUltronUiTest {
        stubListData()
        val clicked = mutableListOf<Pair<String, String>>()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationListScreen(
                    vm = createVm(),
                    onBack = {},
                    onCourseClick = { c, t -> clicked.add(c to t) },
                    onEvaluationDetail = {},
                    onSubmitClick = {},
                )
            }
        }
        waitText("高等数学")
        onNodeWithText("高等数学").performClick()
        waitUntil(timeoutMillis = 2_000) { clicked.isNotEmpty() }
        assertEquals("高等数学", clicked[0].first)
        assertEquals("张三", clicked[0].second)
    }

    @Test
    fun 写评价按钮回调onSubmitClick() = runUltronUiTest {
        stubListData()
        var submitted = false
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationListScreen(
                    vm = createVm(),
                    onBack = {},
                    onCourseClick = { _, _ -> },
                    onEvaluationDetail = {},
                    onSubmitClick = { submitted = true },
                )
            }
        }
        waitText("课程评价")
        onNodeWithContentDescription("写评价").performClick()
        waitUntil(timeoutMillis = 2_000) { submitted }
    }

    @Test
    fun Tab切换到我的评价() = runUltronUiTest {
        stubListData()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationListScreen(
                    vm = createVm(),
                    onBack = {},
                    onCourseClick = { _, _ -> },
                    onEvaluationDetail = {},
                    onSubmitClick = {},
                )
            }
        }
        waitText("全部评价")
        onNodeWithText("我的评价").performClick()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("搜索课程 / 内容").fetchSemanticsNodes().isNotEmpty()
        }
    }





    @Test
    fun 详情页渲染评价信息() = runUltronUiTest {
        everySuspend { port.getEvaluation("e1") } returns sampleEvaluation
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationDetailScreen(vm = createVm(), evaluationId = "e1", onBack = {}, onEditEvaluation = {})
            }
        }
        waitText("评价详情")
        waitText("高等数学")
        onNodeWithText("张三").assertIsDisplayed()
        onNodeWithText("讲得不错，作业量适中").assertIsDisplayed()
        onNodeWithText("小明").assertIsDisplayed()
        onNodeWithText("/ 5 分").assertIsDisplayed()
    }

    @Test
    fun 写评价页渲染表单分组() = runUltronUiTest {


        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationSubmitScreen(
                    vm = createVm(
                        listOf(
                            Course(
                                name = "高等数学",
                                location = "教学楼101",
                                teacher = "张三",
                                dayOfWeek = 1,
                                startSection = 1,
                                sectionCount = 2,
                                weeks = listOf(1, 2, 3, 4),
                                color = "#FF5722",
                            ),
                        ),
                    ),
                    onBack = {},
                    onSubmitted = {},
                )
            }
        }
        waitText("课程")

        onAllNodesWithText("评价内容")[0].assertIsDisplayed()
        onAllNodesWithText("署名")[0].assertIsDisplayed()
        onAllNodesWithText("选择课程")[0].assertIsDisplayed()
    }


}
