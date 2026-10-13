@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.atiurin.ultron.core.compose.runUltronUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnnouncementImageViewerScreenUiTest {

    private val messages = mutableListOf<String>()

    private fun androidx.compose.ui.test.ComposeUiTest.setContentFor(
        url: String = "https://example.com/a.png",
        canSaveImage: Boolean = true,
        saveImage: suspend (String) -> Boolean = { true },
        onBack: () -> Unit = {},
    ) {
        setContent {
            AnnouncementImageViewerScreen(
                url = url,
                alt = "示例图",
                canSaveImage = canSaveImage,
                saveImage = saveImage,
                showMessage = { messages.add(it) },
                onBack = onBack,
            )
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `允许保存时显示保存按钮`() = runUltronUiTest {
        setContentFor(canSaveImage = true)

        waitText("保存")
        onNodeWithText("保存").assertIsDisplayed()
    }

    @Test
    fun `禁止保存时不显示保存按钮`() = runUltronUiTest {
        setContentFor(canSaveImage = false)


        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithContentDescription("关闭").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(onAllNodesWithText("保存").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `点击保存成功后回调已保存消息`() = runUltronUiTest {
        setContentFor(canSaveImage = true, saveImage = { true })

        waitText("保存")
        onNodeWithText("保存").performClick()
        waitUntil(timeoutMillis = 5_000) { messages.isNotEmpty() }
        assertEquals(listOf("已保存到相册"), messages)
    }

    @Test
    fun `保存失败时回调失败消息`() = runUltronUiTest {
        setContentFor(canSaveImage = true, saveImage = { false })

        waitText("保存")
        onNodeWithText("保存").performClick()
        waitUntil(timeoutMillis = 5_000) { messages.isNotEmpty() }
        assertEquals(listOf("保存失败，请重试"), messages)
    }

    @Test
    fun `保存进行中显示保存中状态`() = runUltronUiTest {
        val gate = CompletableDeferred<Boolean>()
        setContentFor(canSaveImage = true, saveImage = { gate.await() })

        waitText("保存")
        onNodeWithText("保存").performClick()
        waitText("保存中")
        onNodeWithText("保存中").assertIsDisplayed()
        gate.complete(true)
        waitUntil(timeoutMillis = 5_000) { messages.isNotEmpty() }
        assertEquals(listOf("已保存到相册"), messages)
    }

    @Test
    fun `点击关闭回调onBack`() = runUltronUiTest {
        var backCount = 0
        setContentFor(onBack = { backCount++ })

        waitText("保存")
        onNodeWithContentDescription("关闭").performClick()
        assertEquals(1, backCount)
    }
}
