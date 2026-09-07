@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package restarhalf.stellar.schedule.ui.screens.announcement

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.atiurin.ultron.core.compose.runUltronUiTest
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import com.russhwolf.settings.ObservableSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import restarhalf.stellar.schedule.domain.usecase.FetchAdConfigUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementUseCase
import restarhalf.stellar.schedule.domain.usecase.FetchAnnouncementsUseCase
import restarhalf.stellar.schedule.domain.usecase.MarkAnnouncementsReadUseCase
import top.yukonga.miuix.kmp.theme.MiuixTheme
import restarhalf.stellar.schedule.ui.viewmodel.AnnouncementViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.ThemeController
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 公告列表页 Compose UI 测试（Ultron KMP 入口）
 *
 * [runUltronUiTest] 是 Ultron 的 KMP 测试入口（块内接收者为 androidx ComposeUiTest）。
 * 注意：Ultron 的 SemanticsMatcher 扩展（hasText("x").click() 等）绑定 ComposeTestRule 环境，
 * 与 runUltronUiTest 的 ComposeUiTest 环境互斥（混用抛 IllegalStateException），
 * 因此块内统一使用标准 ComposeUiTest API，异步等待用 waitUntil。
 * 另外严禁嵌套调用 runUltronUiTest（内层 afterTest 会重置 ComposeRootRegistry）。
 *
 * 数据层与单测同策略：mock [AnnouncementPort] 与 [ObservableSettings]，
 * ViewModel / UseCase / Store 全部真实实现，验证从数据到渲染的完整链路。
 * 真机测试不注入 setMain（会破坏 lifecycle 主线程检查），viewModelScope 走原生主线程。
 */
@RunWith(AndroidJUnit4::class)
class AnnouncementListScreenUiTest {

    private val settings = mock<ObservableSettings>(MockMode.autofill) {
        every { putString(any(), any()) } returns Unit
        every { putLong(any(), any()) } returns Unit
    }
    private val port = mock<AnnouncementPort>()

    private fun makeViewModel(): AnnouncementViewModel {
        return AnnouncementViewModel(
            fetchAnnouncements = FetchAnnouncementsUseCase(port, AnnouncementStore(settings)),
            markAnnouncementsRead = MarkAnnouncementsReadUseCase(AnnouncementStore(settings)),
            fetchAdConfig = FetchAdConfigUseCase(port),
            fetchAnnouncement = FetchAnnouncementUseCase(port),
        )
    }

    // 注意：这里绝不能再用 runUltronUiTest 包装（会嵌套 runComposeUiTest，
    // 内层 afterTest 会重置 ComposeRootRegistry，导致外层所有节点查询失败）
    private fun androidx.compose.ui.test.ComposeUiTest.setContentFor(
        vm: AnnouncementViewModel,
        onAnnouncementClick: (String) -> Unit = {},
    ) {
        setContent {
            MiuixTheme(controller = ThemeController(ColorSchemeMode.Light)) {
                AnnouncementListScreen(
                    vm = vm,
                    onBack = {},
                    onAnnouncementClick = onAnnouncementClick,
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
    fun 列表渲染公告标题与置顶标记() = runUltronUiTest {
        val pinned = Announcement(id = "1", title = "新版发布", pinned = true, createdAt = 1000L)
        val normal = Announcement(id = "2", title = "常规公告", createdAt = 2000L)
        everySuspend { port.listAnnouncements() } returns listOf(pinned, normal)
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm)

        waitText("新版发布")
        onNodeWithText("新版发布").assertIsDisplayed()
        onNodeWithText("常规公告").assertIsDisplayed()
    }

    @Test
    fun 点击公告条目回调公告ID() = runUltronUiTest {
        val clicked = mutableListOf<String>()
        val announcement = Announcement(id = "ann-42", title = "点我看看", createdAt = 1000L)
        everySuspend { port.listAnnouncements() } returns listOf(announcement)
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm) { clicked.add(it) }

        waitText("点我看看")
        onNodeWithText("点我看看").performClick()
        assertEquals(listOf("ann-42"), clicked)
    }

    @Test
    fun 加载失败时显示错误提示条() = runUltronUiTest {
        everySuspend { port.listAnnouncements() } throws RuntimeException("网络开小差")
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm)

        waitText("网络开小差")
        onNodeWithText("网络开小差").assertIsDisplayed()
    }

    @Test
    fun 空列表时显示暂无公告占位() = runUltronUiTest {
        everySuspend { port.listAnnouncements() } returns emptyList()
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm)

        waitText("暂无公告")
        onNodeWithText("暂无公告").assertIsDisplayed()
    }

    @Test
    fun 列表可滚动定位到指定条目() = runUltronUiTest {
        val items = (1..30).map {
            Announcement(id = "id-$it", title = "公告 $it", createdAt = it.toLong() * 1000)
        }
        everySuspend { port.listAnnouncements() } returns items
        everySuspend { port.getAdConfig() } returns null
        val vm = makeViewModel()

        setContentFor(vm)

        waitText("公告 1")
        onNode(hasScrollAction()).performScrollToNode(hasText("公告 30"))
        onNodeWithText("公告 30").assertIsDisplayed()
    }
}
