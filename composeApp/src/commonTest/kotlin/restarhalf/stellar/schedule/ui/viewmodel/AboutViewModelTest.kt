package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort
import restarhalf.stellar.schedule.core.update.DEFAULT_QQ_GROUP_KEY
import restarhalf.stellar.schedule.domain.usecase.CheckAppUpdateUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * AboutViewModel 单元测试。
 *
 * CheckAppUpdateUseCase 是 final class 无法 mock，用真实实例 + mock AppUpdatePort。
 *
 * 两个关键点（与裸 MutableStateFlow 型 ViewModel 不同）：
 * 1. uiState 是 stateIn(WhileSubscribed)——测试必须先订阅才能驱动 combine 上游，
 *    否则 uiState.value 永远停在 initialValue；
 * 2. checkUpdate 内部 withContext(Dispatchers.IO)，协程跨线程恢复——Main dispatcher
 *    用 @BeforeTest 类级安装，避免协程在 resetMain 之后恢复触发 ISE。
 */
class AboutViewModelTest {

    private val appUpdate = mock<AppUpdatePort>(MockMode.autofill)

    private val info = AppUpdateInfo(
        latestVersion = "1.2.0",
        releasePageUrl = "https://example.com/release",
        downloadUrl = "https://example.com/app.apk",
        changelog = "更新",
    )

    /** 类级持有 Main dispatcher，测试中可主动 drain 其内部任务队列 */
    private val mainDispatcher = UnconfinedTestDispatcher()

    private fun makeViewModel() = AboutViewModel(
        checkAppUpdate = CheckAppUpdateUseCase(appUpdate),
    )

    /** 驱动 stateIn(WhileSubscribed)：订阅 uiState，combine 上游才开始运行 */
    private fun kotlinx.coroutines.test.TestScope.subscribeUi(vm: AboutViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }
    }

    /** 真实时间轮询等待 uiState 就绪（checkUpdate 走真实 IO 线程），每轮 drain Main 队列 */
    private suspend fun awaitState(
        vm: AboutViewModel,
        timeoutMs: Long = 5_000,
        predicate: (AboutViewModel.AboutUiState) -> Boolean,
    ): AboutViewModel.AboutUiState {
        val deadline = Clock.System.now().toEpochMilliseconds() + timeoutMs
        var state = vm.uiState.value
        while (!predicate(state) && Clock.System.now().toEpochMilliseconds() < deadline) {
            mainDispatcher.scheduler.advanceUntilIdle()
            withContext(Dispatchers.Default) { delay(10) }
            state = vm.uiState.value
        }
        return state
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `buildScreenUi 正常版本显示版本号且可检查更新`() = runTest {
        val vm = makeViewModel()

        val ui = vm.buildScreenUi(isInPreview = false, versionName = "1.0.0", updateChecking = false, updateSummary = "检查")

        assertEquals("1.0.0", ui.versionDisplay)
        assertEquals("1.0.0", ui.currentVersionForCheck)
        assertEquals("检查", ui.updateActionSummary)
        assertTrue(ui.canCheckUpdate)
    }

    @Test
    fun `buildScreenUi 预览版隐藏版本号且检查中不可重复触发`() = runTest {
        val vm = makeViewModel()

        val preview = vm.buildScreenUi(isInPreview = true, versionName = "1.0.0", updateChecking = false, updateSummary = "检查")
        val checking = vm.buildScreenUi(isInPreview = false, versionName = "1.0.0", updateChecking = true, updateSummary = "检查")

        assertEquals("预览版", preview.versionDisplay)
        assertEquals("", preview.currentVersionForCheck)
        assertEquals("检查中...", checking.updateActionSummary)
        assertFalse(checking.canCheckUpdate)
    }

    @Test
    fun `checkUpdate 发现新版本时更新摘要与pendingUpdate`() = runTest {
        everySuspend { appUpdate.check(any(), any()) } returns info
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.checkUpdate("1.0.0")
        val state = awaitState(vm) { it.pendingUpdate != null }

        assertEquals("发现新版本 1.2.0", state.updateSummary)
        assertEquals(info, state.pendingUpdate)
        assertFalse(state.updateChecking)
        verifySuspend(VerifyMode.exactly(1)) { appUpdate.check("1.0.0", null) }
    }

    @Test
    fun `checkUpdate 已是最新版本时摘要带当前版本号`() = runTest {
        everySuspend { appUpdate.check(any(), any()) } returns null
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.checkUpdate("1.0.0")
        val state = awaitState(vm) { it.updateSummary != "检查" }

        assertEquals("已是最新版本（1.0.0）", state.updateSummary)
        assertNull(state.pendingUpdate)
    }

    @Test
    fun `checkUpdate 异常时置失败摘要并复位checking`() = runTest {
        everySuspend { appUpdate.check(any(), any()) } throws RuntimeException("network down")
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.checkUpdate("1.0.0")
        val state = awaitState(vm) { !it.updateChecking && it.updateSummary != "检查" }

        assertFalse(state.updateChecking)
        assertTrue(state.updateSummary.isNotEmpty(), "异常后摘要应更新")
        assertNull(state.pendingUpdate)
    }

    @Test
    fun `clearPendingUpdate 清除待处理更新`() = runTest {
        everySuspend { appUpdate.check(any(), any()) } returns info
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.checkUpdate("1.0.0")
        awaitState(vm) { it.pendingUpdate != null }
        vm.clearPendingUpdate()

        assertNull(vm.uiState.value.pendingUpdate)
    }

    @Test
    fun `requestOpenGithub发出OpenUri事件`() = runTest {
        val vm = makeViewModel()
        val received = mutableListOf<AboutViewModel.AboutUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { received += it }
        }

        vm.requestOpenGithub()

        assertEquals<List<AboutViewModel.AboutUiEvent>>(
            listOf(AboutViewModel.AboutUiEvent.OpenUri("https://github.com/restarhalf/Chrnova")),
            received,
        )
    }

    @Test
    fun `requestJoinDefaultQqGroup发出默认群key`() = runTest {
        val vm = makeViewModel()
        val received = mutableListOf<AboutViewModel.AboutUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { received += it }
        }

        vm.requestJoinDefaultQqGroup()

        assertEquals<List<AboutViewModel.AboutUiEvent>>(
            listOf(AboutViewModel.AboutUiEvent.JoinQqGroup(DEFAULT_QQ_GROUP_KEY)),
            received,
        )
    }

    @Test
    fun `requestWxPayAward发出微信赞赏请求事件`() = runTest {
        val vm = makeViewModel()
        val received = mutableListOf<AboutViewModel.AboutUiEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.events.collect { received += it }
        }

        vm.requestWxPayAward()

        assertEquals<List<AboutViewModel.AboutUiEvent>>(
            listOf(AboutViewModel.AboutUiEvent.WxPayAwardRequested),
            received,
        )
    }
}
