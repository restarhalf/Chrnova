@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.atiurin.ultron.core.compose.runUltronUiTest
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import restarhalf.stellar.schedule.domain.model.CardEdgeThickness
import restarhalf.stellar.schedule.domain.model.CardRefractionLevel
import restarhalf.stellar.schedule.domain.port.BackgroundSettingsPort
import restarhalf.stellar.schedule.domain.port.WallpaperPort
import restarhalf.stellar.schedule.ui.viewmodel.BackgroundViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class CustomizeOverlayUiTest {

    private val port = mock<BackgroundSettingsPort>(MockMode.autofill)
    private val wallpaperPort = mock<WallpaperPort>(MockMode.autofill)

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

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 渲染搭配页顶栏与工具按钮() = runUltronUiTest {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CustomizeOverlay(
                    vm = makeViewModel(),
                    scale = 0.65f,
                    offsetY = 0.dp,
                    selectedPage = 1,
                    onSelectPage = {},
                    onSheetOpenChange = {},
                    onDraftChange = {},
                    exiting = false,
                    onRequestClose = {},
                )
            }
        }
        waitText("取消")
        onNodeWithText("应用").assertExists()
        onNodeWithContentDescription("换壁纸").assertExists()
        onNodeWithContentDescription("效果").assertExists()
        onNodeWithContentDescription("自定义").assertExists()
        onNodeWithContentDescription("今日").assertExists()
        onNodeWithContentDescription("课表").assertExists()
        onNodeWithContentDescription("考务").assertExists()
        onNodeWithContentDescription("体测").assertExists()
    }

    @Test
    fun 效果面板里可调整滑杆并清除壁纸() = runUltronUiTest {
        every { wallpaperPort.peekWallpaper() } returns ImageBitmap(8, 8)
        everySuspend { wallpaperPort.clearWallpaper() } returns Unit
        val vm = makeViewModel()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CustomizeOverlay(
                    vm = vm,
                    scale = 0.65f,
                    offsetY = 0.dp,
                    selectedPage = 1,
                    onSelectPage = {},
                    onSheetOpenChange = {},
                    onDraftChange = {},
                    exiting = false,
                    onRequestClose = {},
                )
            }
        }
        waitText("取消")

        onNodeWithContentDescription("效果").performClick()
        waitText("壁纸亮度")
        onNodeWithText("壁纸模糊").assertExists()
        onNodeWithText("背景不透明度").assertExists()
        onNodeWithText("组件不透明度").assertExists()
        onNodeWithText("卡片模糊").assertExists()
        onNodeWithText("卡片折射").assertExists()
        onNodeWithText("边缘厚度").assertExists()
        onNodeWithText("高光描边").assertExists()

        onNodeWithContentDescription("关闭").performClick()
        waitText("取消")
        onNodeWithContentDescription("清除壁纸").performClick()
        verifySuspend(VerifyMode.exactly(1)) { wallpaperPort.clearWallpaper() }
    }
}
