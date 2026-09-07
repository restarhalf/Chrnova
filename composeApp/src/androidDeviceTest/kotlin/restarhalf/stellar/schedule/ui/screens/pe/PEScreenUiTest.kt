@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.pe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
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
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.dsl.module
import restarhalf.stellar.schedule.data.remote.PEAuthProfileResponse
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEDetailResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PEScoreListResponse
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryData
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryItem
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryResponse
import restarhalf.stellar.schedule.data.remote.PESubjectScore
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.domain.model.JwxtAuthProfile
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.PEAuthProfileUseCase
import restarhalf.stellar.schedule.domain.usecase.PELoginUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreListUseCase
import restarhalf.stellar.schedule.domain.usecase.PESubjectScoreHistoryUseCase
import restarhalf.stellar.schedule.ui.port.ScreenTunerPort
import restarhalf.stellar.schedule.ui.viewmodel.PELoginViewModel
import restarhalf.stellar.schedule.ui.viewmodel.PEViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 体育模块 Compose UI 测试（Ultron KMP 入口），覆盖 5 个屏幕。
 *
 * VM 构造同 JVM 测试配方：4 个 UseCase 真实实例 + mock PEGateway/PEAuthPort/PEAuthWorkflowPort。
 * isLoggedIn 响应 peAuth.observeToken()——token 非空才触发页面 LaunchedEffect 加载数据。
 * PEQRCodeScreen 经 koinInject 获取 ScreenTunerPort，测试需先 ensureKoin 注册 mock。
 */
class PEScreenUiTest {

    private val peGateway = mock<PEGateway>(MockMode.autofill)
    private val peAuth = mock<PEAuthPort>(MockMode.autofill)
    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    private val tokenFlow = MutableStateFlow("")
    private val cachedProfileFlow = MutableStateFlow(PEAuthProfile())

    private val screenTuner = mock<ScreenTunerPort>(MockMode.autofill)

    private companion object {
        @Volatile
        private var koinReady = false
    }

    /** PEQRCodeScreen 用 koinInject 获取 ScreenTunerPort。
     *  全量回归时 ExclusionScreenUiTest 可能已 startKoin（无 ScreenTunerPort 定义），
     *  故已有 Koin 时用 loadKoinModules 补注册；标志防止重复加载覆盖异常。 */
    private fun ensureKoin() {
        if (koinReady) return
        val peModule = module { single<ScreenTunerPort> { screenTuner } }
        if (GlobalContext.getOrNull() == null) {
            startKoin { modules(peModule) }
        } else {
            loadKoinModules(peModule)
        }
        koinReady = true
    }

    private fun makeViewModel(): PEViewModel {
        every { peAuth.observeToken() } returns tokenFlow
        every { peAuth.observeProfile() } returns cachedProfileFlow
        return PEViewModel(
            peScoreListUseCase = PEScoreListUseCase(peGateway, peAuthWorkflow),
            peScoreDetailUseCase = PEScoreDetailUseCase(peGateway, peAuthWorkflow),
            peSubjectScoreHistoryUseCase = PESubjectScoreHistoryUseCase(peGateway, peAuthWorkflow),
            peAuthProfileUseCase = PEAuthProfileUseCase(peGateway, peAuth, peAuthWorkflow),
            peAuth = peAuth,
            peAuthWorkflow = peAuthWorkflow,
        )
    }

    private fun makeLoginViewModel() = PELoginViewModel(
        peLoginUseCase = PELoginUseCase(peAuthWorkflow),
    )

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // region PEScoreScreen

    @Test
    fun 未登录显示登录入口() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEScoreScreen(vm = makeViewModel(), onNavigateToDetail = {}, onLogin = {})
            }
        }
        waitText("体测")
        onNodeWithText("账号").assertIsDisplayed()
        onNodeWithText("登录").assertIsDisplayed()
        onNodeWithText("用于获取体测成绩").assertIsDisplayed()
    }

    @Test
    fun 已登录渲染体测成绩列表与二维码入口() = runUltronUiTest {
        tokenFlow.value = "tok"
        everySuspend { peGateway.getScoreList() } returns PEScoreListResponse(
            dataArr = listOf(
                PEYearScore(schoolYear = "2025-2026", total = 95.0, done = 8, nums = 10),
                PEYearScore(schoolYear = "2024-2025", total = 80.0, done = 5, nums = 10, isFree = 1),
            )
        )
        everySuspend { peGateway.getProfile() } returns PEAuthProfileResponse(status = "ok")
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEScoreScreen(vm = makeViewModel(), onNavigateToDetail = {}, onLogin = {})
            }
        }
        waitText("体测")
        waitText("2025-2026学年")
        onNodeWithText("已测 8/10").assertIsDisplayed()
        onNodeWithText("95.0分").assertIsDisplayed()
        onNodeWithText("免测").assertIsDisplayed()
        // 已登录 → 顶栏出现二维码入口（Icon 的语义是 ContentDescription）
        onNode(hasContentDescription("二维码")).assertIsDisplayed()
    }

    // endregion

    // region PELoginScreen

    @Test
    fun 登录页渲染与空凭据门禁() = runUltronUiTest {
        val vm = makeLoginViewModel()
        var successCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PELoginScreen(
                    vm = vm,
                    onBack = {},
                    onLoginSuccess = { successCalls++ },
                    inWel = false,
                    next = {},
                )
            }
        }
        waitText("登录体测系统")
        onNodeWithText("学号").assertIsDisplayed()
        onNodeWithText("密码").assertIsDisplayed()
        onNodeWithText("此为体测平台账号登录").assertIsDisplayed()
        // 空凭据 → 登录按钮禁用，点击无回调
        onNodeWithText("登录").performClick()
        Thread.sleep(500)
        assertEquals(0, successCalls)
        // 填写凭据后点击登录 → 成功回调触发
        everySuspend { peAuthWorkflow.login(any(), any()) } returns Unit
        vm.onUsernameChange("2023001")
        vm.onPasswordChange("pwd")
        runOnIdle { }
        onNodeWithText("登录").performClick()
        waitUntil(timeoutMillis = 3_000) { successCalls == 1 }
    }

    // endregion

    // region PEQRCodeScreen

    @Test
    fun 二维码页渲染学生信息() = runUltronUiTest {
        ensureKoin()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEQRCodeScreen(
                    vm = makeViewModel(),
                    jwxtAuthProfile = JwxtAuthProfile(name = "张三", userNo = "2023001"),
                    onBack = {},
                )
            }
        }
        waitText("请将此二维码展示给体测老师扫描")
        // 顶栏标题与卡片标题重复 → 取首个节点
        onAllNodesWithText("体测二维码")[0].assertIsDisplayed()
        onNodeWithContentDescription("体测二维码").assertIsDisplayed()
        onNodeWithText("2023001").assertIsDisplayed()
        onNodeWithText("张三").assertIsDisplayed()
    }

    // endregion

    // region PEDetailScreen / PESubjectHistoryScreen

    @Test
    fun 详情页渲染总分与科目并回调点击() = runUltronUiTest {
        tokenFlow.value = "tok"
        everySuspend { peGateway.getScoreDetail("2025-2026") } returns PEDetailResponse(
            data = PEDetailData(
                totalScore = 90.0,
                totalGrade = "优秀",
                dataArr = listOf(
                    PESubjectScore(
                        subjectId = "S1",
                        subName = "50米跑",
                        result = "7.5",
                        score = 75,
                        unit = "s",
                        subRatio = "10",
                        grade = "良好",
                        isJoin = 1,
                    ),
                ),
            )
        )
        var clickedSubjectId: String? = null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PEDetailScreen(
                    vm = makeViewModel(),
                    schoolYear = "2025-2026",
                    onSubjectClick = { clickedSubjectId = it.subjectId },
                    onBack = {},
                )
            }
        }
        waitText("2025-2026学年体测成绩")
        onNodeWithText("总分").assertIsDisplayed()
        onNodeWithText("90.0分").assertIsDisplayed()
        onNodeWithText("优秀").assertIsDisplayed()
        onNodeWithText("50米跑").assertIsDisplayed()
        onNodeWithText("75分").assertIsDisplayed()
        onNodeWithText("10%").assertIsDisplayed()
        onNodeWithText("50米跑").performClick()
        waitUntil(timeoutMillis = 3_000) { clickedSubjectId == "S1" }
    }

    @Test
    fun 单科历史渲染记录与当前成绩标记() = runUltronUiTest {
        tokenFlow.value = "tok"
        everySuspend { peGateway.getSubjectScoreHistory(any(), any(), any(), any()) } returns
            PESubjectHistoryResponse(
                data = PESubjectHistoryData(
                    dataList = listOf(
                        PESubjectHistoryItem(
                            result = "7.5",
                            sessionName = "第一场",
                            scoreTime = "2025-10-01",
                            sourceScoreId = "ss1",
                        ),
                    ),
                    totalRows = 1,
                )
            )
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                PESubjectHistoryScreen(
                    vm = makeViewModel(),
                    schoolYear = "2025-2026",
                    subjectId = "S1",
                    subjectName = "50米跑",
                    unit = "s",
                    currentResult = "7.5",
                    onBack = {},
                )
            }
        }
        waitText("50米跑成绩记录")
        waitText("共1条记录 · 按时间倒序")
        onNodeWithText("7.5s").assertIsDisplayed()
        // currentResult 与记录 result 一致 → 显示"当前成绩"标记
        onNodeWithText("当前成绩").assertIsDisplayed()
        onNodeWithText("考核场次").assertIsDisplayed()
        onNodeWithText("第一场").assertIsDisplayed()
        onNodeWithText("2025-10-01").assertIsDisplayed()
    }

    // endregion
}
