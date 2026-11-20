package restarhalf.stellar.schedule.ui.viewmodel

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import restarhalf.stellar.schedule.domain.model.CardEdgeThickness
import restarhalf.stellar.schedule.domain.model.CardRefractionLevel
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import restarhalf.stellar.schedule.domain.port.WallpaperPort
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BackgroundViewModelTest {

    private val port = mock<BackgroundSettingsPort>(MockMode.autofill)
    private val wallpaperPort = mock<WallpaperPort>(MockMode.autofill)

    private val mainDispatcher = UnconfinedTestDispatcher()

    private val alphaFlow = MutableStateFlow(0.6f)
    private val blurFlow = MutableStateFlow(0.2f)
    private val brightnessFlow = MutableStateFlow(0f)
    private val componentsAlphaFlow = MutableStateFlow(0.8f)
    private val cardBlurPxFlow = MutableStateFlow(12f)
    private val cardRefractionFlow = MutableStateFlow(CardRefractionLevel.DEFAULT)
    private val cardEdgeThicknessFlow = MutableStateFlow(CardEdgeThickness.DEFAULT)
    private val cardRimFlow = MutableStateFlow(true)

    private fun stubFlows() {
        every { port.observeBackgroundAlpha() } returns alphaFlow
        every { port.observeBackgroundBlur() } returns blurFlow
        every { port.observeBackgroundBrightness() } returns brightnessFlow
        every { port.observeComponentsAlpha() } returns componentsAlphaFlow
        every { port.observeCardBlurPx() } returns cardBlurPxFlow
        every { port.observeCardRefraction() } returns cardRefractionFlow
        every { port.observeCardEdgeThickness() } returns cardEdgeThicknessFlow
        every { port.observeCardRim() } returns cardRimFlow
        every { wallpaperPort.peekWallpaper() } returns null
        everySuspend { wallpaperPort.loadWallpaper() } returns null
    }

    private fun makeViewModel(): BackgroundViewModel {
        stubFlows()
        return BackgroundViewModel(port, wallpaperPort)
    }

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

        assertEquals(null, state.wallpaper)
        assertEquals(null, state.wallpaperPath)
        assertEquals(0.6f, state.backgroundAlpha)
        assertEquals(0.2f, state.backgroundBlur)
        assertEquals(0f, state.backgroundBrightness)
        assertEquals(0.8f, state.componentsAlpha)
    }

    @Test
    fun `observe流变化时uiState响应式更新`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        blurFlow.value = 0.9f
        brightnessFlow.value = 12f
        advanceMain()

        assertEquals(0.9f, vm.uiState.value.backgroundBlur)
        assertEquals(12f, vm.uiState.value.backgroundBrightness)
    }

    @Test
    fun `onWallpaperPicked透传壁纸端口`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.onWallpaperPicked("content://picked")
        advanceMain()

        verifySuspend(VerifyMode.exactly(1)) { wallpaperPort.importWallpaper("content://picked") }
    }

    @Test
    fun `onWallpaperCleared清空壁纸`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.onWallpaperCleared()
        advanceMain()

        verifySuspend(VerifyMode.exactly(1)) { wallpaperPort.clearWallpaper() }
    }

    @Test
    fun `保存壁纸位置写入端口并更新状态`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.onWallpaperTransformChanged(Offset(12f, -8f), 1.5f)
        advanceMain()

        verify(VerifyMode.exactly(1)) { port.setWallpaperOffset(12f, -8f) }
        verify(VerifyMode.exactly(1)) { port.setWallpaperScale(1.5f) }
        assertEquals(Offset(12f, -8f), vm.uiState.value.wallpaperOffset)
        assertEquals(1.5f, vm.uiState.value.wallpaperScale)
    }

    @Test
    fun `换壁纸重置位置并记录壁纸明暗`() = runTest {
        val bitmap = mock<ImageBitmap>(MockMode.autofill)
        everySuspend { wallpaperPort.importWallpaper("content://picked") } returns bitmap
        every { wallpaperPort.wallpaperIsLight(bitmap) } returns false
        every { wallpaperPort.wallpaperFilePath() } returns "/tmp/wallpaper.webp"
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.onWallpaperPicked("content://picked")
        advanceMain()

        verify(VerifyMode.exactly(1)) { port.setWallpaperOffset(0f, 0f) }
        verify(VerifyMode.exactly(1)) { port.setWallpaperScale(0f) }
        verify(VerifyMode.exactly(1)) { port.setWallpaperIsDark(true) }
        assertEquals(bitmap, vm.uiState.value.wallpaper)
        assertEquals(true, vm.uiState.value.wallpaperIsDark)
    }

    @Test
    fun `四个滑块回调透传端口`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.onBackgroundAlphaChanged(0.5f)
        vm.onBackgroundBlurChanged(0.3f)
        vm.onBackgroundBrightnessChanged(-20f)
        vm.onComponentsAlphaChanged(0.7f)

        verify(VerifyMode.exactly(1)) { port.setBackgroundAlpha(0.5f) }
        verify(VerifyMode.exactly(1)) { port.setBackgroundBlur(0.3f) }
        verify(VerifyMode.exactly(1)) { port.setBackgroundBrightness(-20f) }
        verify(VerifyMode.exactly(1)) { port.setComponentsAlpha(0.7f) }
    }

    @Test
    fun `卡片玻璃设置项回调透传端口`() = runTest {
        val vm = makeViewModel()
        subscribeUi(vm)

        vm.onCardBlurPxChanged(20f)
        vm.onCardRefractionChanged(CardRefractionLevel.EXTREME)
        vm.onCardEdgeThicknessChanged(CardEdgeThickness.SMALLER)
        vm.onCardRimChanged(false)

        verify(VerifyMode.exactly(1)) { port.setCardBlurPx(20f) }
        verify(VerifyMode.exactly(1)) { port.setCardRefraction(CardRefractionLevel.EXTREME) }
        verify(VerifyMode.exactly(1)) { port.setCardEdgeThickness(CardEdgeThickness.SMALLER) }
        verify(VerifyMode.exactly(1)) { port.setCardRim(false) }
    }
}
