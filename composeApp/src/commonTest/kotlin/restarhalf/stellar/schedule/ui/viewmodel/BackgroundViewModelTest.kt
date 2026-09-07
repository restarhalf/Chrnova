package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * BackgroundViewModel 单元测试。
 *
 * uiState 为 stateIn(WhileSubscribed)，测试中用 backgroundScope + UnconfinedTestDispatcher
 * 订阅驱动 combine；observe 流用 MutableStateFlow 模拟，可验证响应式更新。
 *
 * Main dispatcher 类级持有并绑定独立 scheduler：某些非可取消协程恢复会进入其内部
 * 任务队列而非内联执行，用 [advanceMain] 主动 drain，避免状态停滞。
 */
class BackgroundViewModelTest {

    private val port = mock<BackgroundSettingsPort>(MockMode.autofill)

    private val mainDispatcher = UnconfinedTestDispatcher()

    private val imageUriFlow = MutableStateFlow<String?>(null)
    private val alphaFlow = MutableStateFlow(0.6f)
    private val blurFlow = MutableStateFlow(0.2f)
    private val componentsAlphaFlow = MutableStateFlow(0.8f)

    private fun stubFlows() {
        every { port.observeBackgroundImageUri() } returns imageUriFlow
        every { port.observeBackgroundAlpha() } returns alphaFlow
        every { port.observeBackgroundBlur() } returns blurFlow
        every { port.observeComponentsAlpha() } returns componentsAlphaFlow
    }

    private fun makeViewModel(): BackgroundViewModel {
        stubFlows()
        return BackgroundViewModel(port)
    }

    /** 订阅 uiState 驱动 stateIn(WhileSubscribed)，并 drain Main 队列使 combine 完成首轮发射 */
    private fun TestScope.subscribeUi(vm: BackgroundViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher()) { vm.uiState.collect {} }
        advanceMain()
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
    fun `订阅后uiState反映backgroundSettings最新值`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        val state = vm.uiState.value

        assertEquals(null, state.backgroundImageUri)
        assertEquals(0.6f, state.backgroundAlpha)
        assertEquals(0.2f, state.backgroundBlur)
        assertEquals(0.8f, state.componentsAlpha)
    }

    @Test
    fun `observe流变化时uiState响应式更新`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        imageUriFlow.value = "content://new-image"
        blurFlow.value = 0.9f
        advanceMain()

        assertEquals("content://new-image", vm.uiState.value.backgroundImageUri)
        assertEquals(0.9f, vm.uiState.value.backgroundBlur)
    }

    @Test
    fun `onBackgroundImageUriChanged透传端口`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.onBackgroundImageUriChanged("content://picked")
        vm.onBackgroundImageUriChanged(null)

        verify(VerifyMode.exactly(1)) { port.setBackgroundImageUri("content://picked") }
        verify(VerifyMode.exactly(1)) { port.setBackgroundImageUri(null) }
    }

    @Test
    fun `三个滑块回调透传端口`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.onBackgroundAlphaChanged(0.5f)
        vm.onBackgroundBlurChanged(0.3f)
        vm.onComponentsAlphaChanged(0.7f)

        verify(VerifyMode.exactly(1)) { port.setBackgroundAlpha(0.5f) }
        verify(VerifyMode.exactly(1)) { port.setBackgroundBlur(0.3f) }
        verify(VerifyMode.exactly(1)) { port.setComponentsAlpha(0.7f) }
    }
}
