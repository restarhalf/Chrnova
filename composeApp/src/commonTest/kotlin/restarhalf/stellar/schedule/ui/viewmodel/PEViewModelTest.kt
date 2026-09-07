package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.data.remote.PEAuthProfile as PEAuthProfileDto
import restarhalf.stellar.schedule.data.remote.PEAuthProfileResponse
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEDetailResponse
import restarhalf.stellar.schedule.data.remote.PEGateway
import restarhalf.stellar.schedule.data.remote.PEScoreListResponse
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryData
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryItem
import restarhalf.stellar.schedule.data.remote.PESubjectHistoryResponse
import restarhalf.stellar.schedule.data.remote.PEYearScore
import restarhalf.stellar.schedule.domain.model.PEAuthProfile
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort
import restarhalf.stellar.schedule.domain.usecase.PEAuthProfileUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreDetailUseCase
import restarhalf.stellar.schedule.domain.usecase.PEScoreListUseCase
import restarhalf.stellar.schedule.domain.usecase.PESubjectScoreHistoryUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/**
 * PEViewModel 单元测试。
 *
 * 4 个 UseCase 均为 final class，用 2 参构造（无 repository）真实实例 + mock PEGateway：
 * observeScoreList/observeDetailData 在 repository 未配置时抛 IllegalStateException，
 * init 的 try-catch 会吞掉——恰好覆盖了"缓存源不可用时 VM 不崩"的健壮性路径。
 *
 * uiState 是裸 MutableStateFlow，无需订阅即可读 value；
 * viewModelScope 内挂起流程用类级 UnconfinedTestDispatcher 的 scheduler drain。
 */
class PEViewModelTest {

    private val peGateway = mock<PEGateway>(MockMode.autofill)
    private val peAuth = mock<PEAuthPort>(MockMode.autofill)
    private val peAuthWorkflow = mock<PEAuthWorkflowPort>(MockMode.autofill)

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    private val tokenFlow = MutableStateFlow("")
    private val cachedProfileFlow = MutableStateFlow(PEAuthProfile())

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

    private fun advanceMain() = mainDispatcher.scheduler.advanceUntilIdle()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init观察缓存档案并更新authProfile`() = runTest {
        cachedProfileFlow.value = PEAuthProfile(stuName = "张三", stdNumber = "2023001")

        val vm = makeViewModel()
        advanceMain()

        assertEquals("张三", vm.uiState.value.authProfile?.stuName)
    }

    @Test
    fun `档案名为空时不覆盖authProfile`() = runTest {
        val vm = makeViewModel()

        cachedProfileFlow.value = PEAuthProfile(stuName = "")
        advanceMain()

        assertNull(vm.uiState.value.authProfile)
    }

    @Test
    fun `loadScoreList成功按学年倒序更新`() = runTest {
        everySuspend { peGateway.getScoreList() } returns PEScoreListResponse(
            dataArr = listOf(
                PEYearScore(schoolYear = "2024-2025", total = 80.0),
                PEYearScore(schoolYear = "2025-2026", total = 95.0),
            )
        )
        val vm = makeViewModel()

        vm.loadScoreList()
        advanceMain()

        val state = vm.uiState.value
        assertEquals(listOf("2025-2026", "2024-2025"), state.yearScores.map { it.schoolYear })
        assertEquals(true, state.loadedScoreList)
        assertFalse(state.loading)
        assertNull(state.error)
    }

    @Test
    fun `loadScoreList失败置error并复位loading`() = runTest {
        everySuspend { peGateway.getScoreList() } throws RuntimeException("网络超时")
        val vm = makeViewModel()

        vm.loadScoreList()
        advanceMain()

        val state = vm.uiState.value
        assertTrue(state.error != null)
        assertFalse(state.loading)
        assertFalse(state.loadedScoreList)
    }

    @Test
    fun `loadScoreDetail成功设置detailData`() = runTest {
        everySuspend { peGateway.getScoreDetail("2025-2026") } returns PEDetailResponse(
            data = PEDetailData(totalScore = 90.0, totalGrade = "优秀")
        )
        val vm = makeViewModel()

        vm.loadScoreDetail("2025-2026")
        advanceMain()

        val state = vm.uiState.value
        assertEquals(90.0, state.detailData?.totalScore)
        assertEquals(true, state.loadedDetail)
        assertFalse(state.loading)
    }

    @Test
    fun `loadScoreDetail失败置error`() = runTest {
        everySuspend { peGateway.getScoreDetail("2025-2026") } throws RuntimeException("服务超时")
        val vm = makeViewModel()

        vm.loadScoreDetail("2025-2026")
        advanceMain()

        assertTrue(vm.uiState.value.error != null)
        assertFalse(vm.uiState.value.loading)
    }

    @Test
    fun `loadSubjectHistory成功填充records`() = runTest {
        everySuspend { peGateway.getSubjectScoreHistory(any(), any(), any(), any()) } returns
            PESubjectHistoryResponse(
                data = PESubjectHistoryData(
                    dataList = listOf(PESubjectHistoryItem(scoreTime = "2025-10-01")),
                    totalRows = 1,
                )
            )
        val vm = makeViewModel()

        vm.loadSubjectHistory(
            schoolYear = "2025-2026",
            subjectId = "S1",
            subjectName = "50米跑",
            unit = "s",
            currentResult = "7.5",
        )
        advanceMain()

        val history = vm.uiState.value.subjectHistory
        assertEquals(1, history?.records?.size)
        assertEquals("2025-10-01", history?.records?.get(0)?.scoreTime)
        assertEquals(true, history?.loaded)
        assertFalse(history?.loading ?: true)
    }

    @Test
    fun `loadSubjectHistory失败兜底清loading`() = runTest {
        everySuspend { peGateway.getSubjectScoreHistory(any(), any(), any(), any()) } throws
            RuntimeException("查询失败")
        val vm = makeViewModel()

        vm.loadSubjectHistory(
            schoolYear = "2025-2026",
            subjectId = "S1",
            subjectName = "50米跑",
            unit = "s",
            currentResult = null,
        )
        advanceMain()

        val history = vm.uiState.value.subjectHistory
        assertEquals(true, history?.loaded)
        assertFalse(history?.loading ?: true)
        assertTrue(vm.uiState.value.error != null)
    }

    @Test
    fun `loadProfile失败置error`() = runTest {
        everySuspend { peGateway.getProfile() } throws RuntimeException("会话失效")
        val vm = makeViewModel()

        vm.loadProfile()
        advanceMain()

        assertTrue(vm.uiState.value.error != null)
    }

    @Test
    fun `logout清空状态并调workflow登出`() = runTest {
        everySuspend { peGateway.getScoreList() } returns PEScoreListResponse(
            dataArr = listOf(PEYearScore(schoolYear = "2025-2026", total = 95.0))
        )
        val vm = makeViewModel()
        vm.loadScoreList()
        advanceMain()
        assertTrue(vm.uiState.value.yearScores.isNotEmpty())

        vm.logout()

        assertEquals(0, vm.uiState.value.yearScores.size)
        assertNull(vm.uiState.value.error)
        verify(VerifyMode.exactly(1)) { peAuthWorkflow.logout() }
    }

    @Test
    fun `isLoggedIn响应token变化`() = runTest {
        val vm = makeViewModel()

        tokenFlow.value = "tok"
        advanceMain()

        val deadline = Clock.System.now().toEpochMilliseconds() + 5000
        while (Clock.System.now().toEpochMilliseconds() < deadline && vm.isLoggedIn.value != true) {
            advanceMain()
            withContext(Dispatchers.Default) { delay(10) }
        }
        assertEquals(true, vm.isLoggedIn.value)

        tokenFlow.value = ""
        advanceMain()
        assertEquals(false, vm.isLoggedIn.value)
    }

    @Test
    fun `buildStatusText错误优先`() = runTest {
        everySuspend { peGateway.getScoreList() } throws RuntimeException("网络超时")
        val vm = makeViewModel()
        vm.loadScoreList()
        advanceMain()

        assertTrue(vm.buildStatusText() != null)
    }

    @Test
    fun `buildStatusText列表空提示暂无体测成绩`() = runTest {
        everySuspend { peGateway.getScoreList() } returns PEScoreListResponse(dataArr = emptyList())
        val vm = makeViewModel()
        vm.loadScoreList()
        advanceMain()

        assertEquals("暂无体测成绩", vm.buildStatusText())
        assertNull(vm.buildStatusText(isDetail = true))
    }

    @Test
    fun `buildStatusText详情空提示暂无体测详情`() = runTest {
        everySuspend { peGateway.getScoreDetail("2025-2026") } returns PEDetailResponse(status = "ok")
        val vm = makeViewModel()
        vm.loadScoreDetail("2025-2026")
        advanceMain()

        assertEquals("暂无体测详情", vm.buildStatusText(isDetail = true))
        assertNull(vm.buildStatusText())
    }

    @Test
    fun `buildSubjectHistoryStatusText空记录提示`() = runTest {
        everySuspend { peGateway.getSubjectScoreHistory(any(), any(), any(), any()) } returns
            PESubjectHistoryResponse(data = PESubjectHistoryData(dataList = emptyList(), totalRows = 0))
        val vm = makeViewModel()

        vm.loadSubjectHistory(
            schoolYear = "2025-2026",
            subjectId = "S1",
            subjectName = "50米跑",
            unit = "s",
            currentResult = null,
        )
        advanceMain()

        assertEquals("暂无50米跑成绩记录", vm.buildSubjectHistoryStatusText())
    }

    @Test
    fun `buildSubjectHistoryStatusText无历史时null`() = runTest {
        val vm = makeViewModel()

        assertNull(vm.buildSubjectHistoryStatusText())
    }
}
