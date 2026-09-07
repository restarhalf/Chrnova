@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import restarhalf.stellar.schedule.domain.repository.GradeRepository
import restarhalf.stellar.schedule.domain.usecase.IsExamNotEndedUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllExaminationsUseCase
import restarhalf.stellar.schedule.domain.usecase.ObserveAllGradesUseCase
import restarhalf.stellar.schedule.domain.usecase.SyncExamEventsToCalendarUseCase
import restarhalf.stellar.schedule.ui.viewmodel.ExaminationViewModel
import restarhalf.stellar.schedule.ui.viewmodel.GradeViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 考务页（考试+成绩双 Tab）Compose UI 测试（Ultron KMP 入口）。
 *
 * 双 VM 手动构造（同 JVM 测试配方），数据经 bindLoader 注入：
 * 页面 LaunchedEffect 自动 load()，考试用未来时间保证"未结束"过滤通过。
 * HorizontalPager 真实挂载，Tab 点击翻页后成绩页内容组合出现。
 */
class EMSScreenUiTest {

    private val auth = mock<JwxtAuthPort>(MockMode.autofill)
    private val settings = mock<SettingsPort>(MockMode.autofill)
    private val examinationRepository = mock<ExaminationRepository>(MockMode.autofill)
    private val gradeRepository = mock<GradeRepository>(MockMode.autofill)
    private val calendarEvent = mock<CalendarEventPort>(MockMode.autofill)

    private val profileFlow = MutableStateFlow(JwxtAuthProfile(userNo = ""))
    private val examsFlow = MutableStateFlow<List<Examination>>(emptyList())
    private val gradesFlow = MutableStateFlow<List<GradeCourse>>(emptyList())
    private val termFlow = MutableStateFlow("")

    private fun makeExamViewModel(): ExaminationViewModel {
        every { auth.observeProfile() } returns profileFlow
        every { examinationRepository.observeAllExaminations() } returns examsFlow
        every { examinationRepository.observeExaminationsByUserNo(any()) } returns examsFlow
        every { settings.observeSelectedTerm() } returns termFlow
        val observeAllExaminations = ObserveAllExaminationsUseCase(examinationRepository, auth)
        return ExaminationViewModel(
            isExamNotEnded = IsExamNotEndedUseCase(),
            observeAllExaminations = observeAllExaminations,
            auth = auth,
            settings = settings,
            syncExamEventsToCalendar = SyncExamEventsToCalendarUseCase(
                observeAllExaminations = observeAllExaminations,
                calendarEvent = calendarEvent,
                settings = settings,
            ),
        )
    }

    private fun makeGradeViewModel(): GradeViewModel {
        every { auth.observeProfile() } returns profileFlow
        every { gradeRepository.observeAllGrades() } returns gradesFlow
        every { settings.observeSelectedTerm() } returns termFlow
        return GradeViewModel(
            observeAllGrades = ObserveAllGradesUseCase(gradeRepository, auth),
            settings = settings,
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染考务标题考试Tab与未结束考试() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EMSScreen(
                    examVm = makeExamViewModel(),
                    gradeVm = makeGradeViewModel(),
                    // ExaminationViewModel.load() 丢弃 loader 返回值（拉取-持久化-观察模式），
                    // loader 必须自行写入 repository observe 流
                    onLoadExaminations = {
                        val list = listOf(
                            Examination(
                                courseName = "大学物理",
                                time = "2027-01-08 14:00-16:00",
                                examinationPlace = "A101",
                            ),
                        )
                        examsFlow.value = list
                        list
                    },
                    onLoadGrades = {
                        TermGradeReport(
                            studentName = "张三",
                            earnedCredits = "120",
                            achievements = listOf(
                                GradeCourse(gradeId = "g1", courseName = "高等数学", score = "95"),
                            ),
                        )
                    },
                )
            }
        }
        waitText("考试")
        waitText("考务")
        onNodeWithText("成绩").assertExists()
        onNode(hasContentDescription("选修学分")).assertExists()
        // 未来时间考试通过"未结束"过滤后渲染
        waitText("大学物理", timeoutMs = 10_000)
        onNodeWithText("地点：A101").assertExists()
    }

    @Test
    fun 切换到成绩Tab显示成绩卡片() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                EMSScreen(
                    examVm = makeExamViewModel(),
                    gradeVm = makeGradeViewModel(),
                    onLoadExaminations = {
                        val list = listOf(
                            Examination(courseName = "大学物理", time = "2027-01-08 14:00-16:00"),
                        )
                        examsFlow.value = list
                        list
                    },
                    onLoadGrades = {
                        TermGradeReport(
                            earnedCredits = "120",
                            achievements = listOf(
                                GradeCourse(gradeId = "g1", courseName = "高等数学", score = "95"),
                            ),
                        )
                    },
                )
            }
        }
        waitText("大学物理")
        // 翻页后成绩页组合出现（高等数学仅存在于成绩页，与考试页课程名区分）
        onNodeWithText("成绩").performClick()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("高等数学").fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithText("高等数学")[0].assertExists()
        onAllNodesWithText("95")[0].assertExists()
    }
}
