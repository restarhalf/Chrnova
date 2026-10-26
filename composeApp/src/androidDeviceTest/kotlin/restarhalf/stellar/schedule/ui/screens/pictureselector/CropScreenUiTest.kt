@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.pictureselector

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isEnabled
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
import org.junit.Assert.assertEquals
import org.junit.Test
import restarhalf.stellar.schedule.pictureselector.CropPreview
import restarhalf.stellar.schedule.pictureselector.CropScreen
import restarhalf.stellar.schedule.pictureselector.ImageSize
import restarhalf.stellar.schedule.pictureselector.PictureSelectorPort
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

class CropScreenUiTest {

    private val port = mock<PictureSelectorPort>(MockMode.autofill)

    private fun stubPreview() {
        every { port.loadCropPreview(any(), any()) } returns CropPreview(
            sourceSize = ImageSize(width = 200, height = 200),
            bitmap = ImageBitmap(width = 200, height = 200),
        )
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitEnabled(
        description: String,
        timeoutMs: Long = 5_000,
    ) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodes(hasContentDescription(description) and isEnabled())
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun 图片加载失败时显示错误文案() = runUltronUiTest {
        every { port.loadCropPreview(any(), any()) } returns null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CropScreen(
                    imageUri = "content://img/1",
                    outputWidthPx = 300,
                    outputHeightPx = 300,
                    port = port,
                    onCancel = {},
                    onCropped = {},
                )
            }
        }
        waitText("无法加载这张图片")
        onNodeWithText("无法加载这张图片").assertIsDisplayed()
        onNodeWithText("裁剪图片").assertIsDisplayed()
    }

    @Test
    fun 加载成功后显示缩放提示与重置按钮() = runUltronUiTest {
        stubPreview()
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CropScreen(
                    imageUri = "content://img/1",
                    outputWidthPx = 300,
                    outputHeightPx = 300,
                    port = port,
                    onCancel = {},
                    onCropped = {},
                )
            }
        }
        waitText("双指缩放 · 双击复位")
        onNodeWithText("双指缩放 · 双击复位").assertIsDisplayed()
        onNodeWithText("1×").assertIsDisplayed()
        onNodeWithContentDescription("重置").assertIsDisplayed()
    }

    @Test
    fun 返回按钮回调onCancel() = runUltronUiTest {
        stubPreview()
        var cancelCalls = 0
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CropScreen(
                    imageUri = "content://img/1",
                    outputWidthPx = 300,
                    outputHeightPx = 300,
                    port = port,
                    onCancel = { cancelCalls++ },
                    onCropped = {},
                )
            }
        }
        waitText("双指缩放 · 双击复位")
        onNodeWithContentDescription("返回").performClick()
        waitUntil(timeoutMillis = 2_000) { cancelCalls == 1 }
    }

    @Test
    fun 确认裁剪写出缓存并回调onCropped() = runUltronUiTest {
        stubPreview()
        every { port.cropAndWriteJpegToCache(any()) } returns "file:///cache/cropped.jpg"
        var croppedUri: String? = null
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                CropScreen(
                    imageUri = "content://img/1",
                    outputWidthPx = 300,
                    outputHeightPx = 300,
                    port = port,
                    onCancel = {},
                    onCropped = { croppedUri = it },
                )
            }
        }
        waitText("双指缩放 · 双击复位")
        waitEnabled("确定")
        onNodeWithContentDescription("确定").performClick()
        waitUntil(timeoutMillis = 5_000) { croppedUri != null }
        assertEquals("file:///cache/cropped.jpg", croppedUri)
    }
}
