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
import com.russhwolf.settings.ObservableSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Announcement
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

/**
 * 公告详情页 UI 测试（Ultron KMP 入口 + 标准 ComposeUiTest API）。
 *
 * 详情页与列表页共享 ViewModel：进入时 load() → selectAnnouncement(id) → markRead。
 * 数据层 mock [AnnouncementPort] 与 [ObservableSettings]，ViewModel/UseCase/Store 真实实现。
 */
@RunWith(AndroidJUnit4::class)
class AnnouncementDetailScreenUiTest {

    /** 本地测试图片路径（@Before 生成 800x600 PNG） */
    private lateinit var imagePath: String

    /**
     * 本地 800x600 PNG（@Before 生成），供 markdown 图片真实加载成功。
     * markdown-renderer 0.45.0 只把「已加载且高度 > 行高*1.5」的图片从 inline
     * 提升为块级 components.image —— 尺寸太小（如 1x1）永远不会走自定义图片组件。
     */
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

    /**
     * map-backed settings：读写闭环（putString/putLong 写入、getStringOrNull/getLong 读回）。
     * 不能用 returns Unit 丢弃写入——详情页 markRead 后 unreadCount 依赖读回 lastReadAtMs。
     */
    private val storage = mutableMapOf<String, Any>()
    private val settings = mock<ObservableSettings>(MockMode.autofill) {
        every { putString(any(), any()) } calls { (key: String, value: String) -> storage[key] = value }
        every { putLong(any(), any()) } calls { (key: String, value: Long) -> storage[key] = value }
        every { getStringOrNull(any()) } calls { (key: String) -> storage[key] as String? }
        every { getLong(any(), any()) } calls { (key: String, defaultValue: Long) -> storage[key] as? Long ?: defaultValue }
    }
    private val port = mock<AnnouncementPort>()

    private fun makeViewModel(): AnnouncementViewModel = AnnouncementViewModel(
        fetchAnnouncements = FetchAnnouncementsUseCase(port, AnnouncementStore(settings)),
        markAnnouncementsRead = MarkAnnouncementsReadUseCase(AnnouncementStore(settings)),
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
        // markdown 图片需加载成功且高度超过行高*1.5 才会提升为块级组件
        // （contentDescription = alt），此处等待其出现在语义树
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithContentDescription("示例图").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithContentDescription("示例图").performClick()
        assertEquals(listOf("file://$imagePath"), clicked)
        // 选中成功后 markAnnouncementRead 推进已读，未读数归零
        waitUntil(timeoutMillis = 5_000) { vm.uiState.value.unreadCount == 0 }
        assertEquals(0, vm.uiState.value.unreadCount)
    }
}
