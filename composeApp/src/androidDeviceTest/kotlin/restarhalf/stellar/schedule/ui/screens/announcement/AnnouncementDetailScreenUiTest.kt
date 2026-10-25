@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.announcement

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atiurin.ultron.core.compose.runUltronUiTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import restarhalf.stellar.schedule.domain.usecase.FetchAdConfigUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementsUseCase
import restarhalf.stellar.schedule.domain.usecase.MarkAnnouncementsReadUseCase
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import java.io.File
import java.io.FileOutputStream


@RunWith(AndroidJUnit4::class)
class AnnouncementDetailScreenUiTest {

    
    private lateinit var imagePath: String

    
    @Before
    fun setUpImage() {
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
        val file = File(dir, "ui-test-image.png")
        val bmp = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.GRAY)
        FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
        bmp.recycle()
        imagePath = file.absolutePath
    }

    
    private val cache = mock<AnnouncementCachePort>(MockMode.autofill) {
        every { getCacheTtlMs() } returns 10L * 60L * 1000L
    }
    private val port = mock<AnnouncementPort>()

    private fun makeViewModel(): AnnouncementViewModel = AnnouncementViewModel(
        fetchAnnouncements = FetchAnnouncementsUseCase(port, cache),
        markAnnouncementsRead = MarkAnnouncementsReadUseCase(cache),
        fetchAdConfig = FetchAdConfigUseCase(port),
        fetchAnnouncement = FetchAnnouncementUseCase(port),
    )

    private fun androidx.compose.ui.test.ComposeUiTest.setContentFor(
        vm: AnnouncementViewModel,
        announcementId: String,
        onImageClick: (String) -> Unit = {},
    ) {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AnnouncementDetailScreen(
                    vm = vm,
                    announcementId = announcementId,
                    onBack = {},
                    onImageClick = onImageClick,
                )
            }
        }
    }

    private fun androidx.compose.ui.test.ComposeUiTest.waitText(text: String, timeoutMs: Long = 5_000) {
        waitUntil(timeoutMillis = timeoutMs) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `渲染标题与正文内容`() = runUltronUiTest {
        val target = Announcement(
            id = "d-1",
            title = "学期安排调整通知",
            content = "自下周起作息时间调整。",
            createdAt = 1_770_000_000L,
        )
        everySuspend { port.listAnnouncements() } returns listOf(target)
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm, "d-1")

        waitText("学期安排调整通知")
        onNodeWithText("学期安排调整通知").assertIsDisplayed()
        onNodeWithText("自下周起作息时间调整。").assertIsDisplayed()
    }

    @Test
    fun `渲染置顶与重要徽章`() = runUltronUiTest {
        val target = Announcement(
            id = "d-2",
            title = "紧急通知",
            content = "正文",
            pinned = true,
            priority = 1,
            createdAt = 1_770_000_000L,
        )
        everySuspend { port.listAnnouncements() } returns listOf(target)
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm, "d-2")

        waitText("紧急通知")
        onNodeWithText("置顶").assertIsDisplayed()
        onNodeWithText("重要").assertIsDisplayed()
    }

    @Test
    fun `空内容显示暂无内容占位`() = runUltronUiTest {
        val target = Announcement(id = "d-3", title = "无内容公告", content = "", createdAt = 1L)
        everySuspend { port.listAnnouncements() } returns listOf(target)
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm, "d-3")

        waitText("暂无内容")
        onNodeWithText("暂无内容").assertIsDisplayed()
    }

    @Test
    fun `加载失败时显示错误信息`() = runUltronUiTest {
        everySuspend { port.listAnnouncements() } throws RuntimeException("网络开小差")
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm, "d-4")

        waitText("网络开小差")
        onNodeWithText("网络开小差").assertIsDisplayed()
    }

    @Test
    fun `点击正文图片回调图片链接并推进已读`() = runUltronUiTest {
        val target = Announcement(
            id = "d-5",
            title = "带图公告",
            content = "![示例图](file://$imagePath)",
            createdAt = 1L,
        )
        everySuspend { port.listAnnouncements() } returns listOf(target)
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()
        val clicked = mutableListOf<String>()

        setContentFor(vm, "d-5") { clicked.add(it) }

        waitText("带图公告")


        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithContentDescription("示例图").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithContentDescription("示例图").performClick()
        assertEquals(listOf("file://$imagePath"), clicked)

        waitUntil(timeoutMillis = 5_000) { vm.uiState.value.unreadCount == 0 }
        assertEquals(0, vm.uiState.value.unreadCount)
    }
}
