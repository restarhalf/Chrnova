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
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.domain.model.EvaluationPage
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.LikeResult
import restarhalf.stellar.schedule.domain.port.CourseEvaluationPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.UserProfilePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.domain.usecase.GetAllCoursesAcrossSemestersUseCase
import restarhalf.stellar.schedule.ui.viewmodel.CourseEvaluationViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class EvaluationCourseScreenUiTest {

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

    private fun createVm(): CourseEvaluationViewModel {
        every { userProfile.getUserNickname() } returns null
        every { auth.observeProfile() } returns profileFlow
        everySuspend { courseRepository.getAllCoursesAcrossSemesters() } returns emptyList()
        return CourseEvaluationViewModel(port, GetAllCoursesAcrossSemestersUseCase(courseRepository), auth, userProfile)
    }

    private fun stubCourseData(evaluations: List<Evaluation> = listOf(sampleEvaluation)) {
        everySuspend { port.listCourseSummaries() } returns listOf(sampleSummary)
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns
            EvaluationPage(items = evaluations, total = evaluations.size)
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染课程头部与评价列表() = runUltronUiTest {
        stubCourseData()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationCourseScreen(
                    vm = createVm(),
                    courseName = "高等数学",
                    teacher = "张三",
                    onBack = {},
                    onEvaluationDetail = {},
                )
            }
        }
        waitText("讲得不错，作业量适中")

        onAllNodesWithText("高等数学")[0].assertIsDisplayed()
        onAllNodesWithText("张三")[0].assertIsDisplayed()
        onNodeWithText("4.5").assertIsDisplayed()
        onNodeWithText("12 条评价").assertIsDisplayed()
        onNodeWithText("讲得不错，作业量适中").assertIsDisplayed()
        onNodeWithText("小明").assertIsDisplayed()
        onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun 匿名评价显示匿名作者() = runUltronUiTest {
        stubCourseData(listOf(sampleEvaluation.copy(anonymous = true, author = "")))
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationCourseScreen(
                    vm = createVm(),
                    courseName = "高等数学",
                    teacher = "张三",
                    onBack = {},
                    onEvaluationDetail = {},
                )
            }
        }
        waitText("讲得不错，作业量适中")
        onNodeWithText("匿名").assertIsDisplayed()
    }

    @Test
    fun 没有聚合数据时头部只显示课程名() = runUltronUiTest {
        everySuspend { port.listCourseSummaries() } returns emptyList()
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } returns
            EvaluationPage(items = listOf(sampleEvaluation), total = 1)
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationCourseScreen(
                    vm = createVm(),
                    courseName = "高等数学",
                    teacher = "张三",
                    onBack = {},
                    onEvaluationDetail = {},
                )
            }
        }
        waitText("讲得不错，作业量适中")
        onAllNodesWithText("高等数学")[0].assertIsDisplayed()
        assertEquals(0, onAllNodesWithText("12 条评价").fetchSemanticsNodes().size)
    }

    @Test
    fun 点击评价条目回调详情ID() = runUltronUiTest {
        stubCourseData()
        var detailId: String? = null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationCourseScreen(
                    vm = createVm(),
                    courseName = "高等数学",
                    teacher = "张三",
                    onBack = {},
                    onEvaluationDetail = { detailId = it },
                )
            }
        }
        waitText("讲得不错，作业量适中")
        onNodeWithText("讲得不错，作业量适中").performClick()
        waitUntil(timeoutMillis = 3_000) { detailId == "e1" }
    }

    @Test
    fun 点赞后计数更新为已赞() = runUltronUiTest {
        profileFlow.value = JwxtAuthProfile(userNo = "2023001")
        stubCourseData()
        everySuspend { port.toggleLike("e1") } returns LikeResult(likes = 4, liked = true)
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationCourseScreen(
                    vm = createVm(),
                    courseName = "高等数学",
                    teacher = "张三",
                    onBack = {},
                    onEvaluationDetail = {},
                )
            }
        }
        waitText("讲得不错，作业量适中")

        onNodeWithContentDescription("点赞").performClick()
        waitText("4")
        onNodeWithText("4").assertIsDisplayed()
        onNodeWithContentDescription("已赞").assertIsDisplayed()
    }

    @Test
    fun 加载失败显示错误提示() = runUltronUiTest {

        everySuspend { port.listCourseSummaries() } throws IllegalStateException("网络异常")
        everySuspend { port.listEvaluations(any(), any(), any(), any()) } throws
            IllegalStateException("网络异常")
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationCourseScreen(
                    vm = createVm(),
                    courseName = "高等数学",
                    teacher = "张三",
                    onBack = {},
                    onEvaluationDetail = {},
                )
            }
        }
        waitText("网络异常")
        onNodeWithText("网络异常").assertIsDisplayed()
    }

    @Test
    fun 返回按钮回调() = runUltronUiTest {
        stubCourseData()
        var backCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EvaluationCourseScreen(
                    vm = createVm(),
                    courseName = "高等数学",
                    teacher = "张三",
                    onBack = { backCalls++ },
                    onEvaluationDetail = {},
                )
            }
        }
        waitText("高等数学")
        onNodeWithContentDescription("返回").performClick()
        waitUntil(timeoutMillis = 2_000) { backCalls == 1 }
        assertEquals(1, backCalls)
    }
}
