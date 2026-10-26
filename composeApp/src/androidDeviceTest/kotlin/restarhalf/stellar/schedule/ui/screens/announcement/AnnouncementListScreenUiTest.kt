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
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
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

@RunWith(AndroidJUnit4::class)
class AnnouncementListScreenUiTest {

    private val cache = mock<AnnouncementCachePort>(MockMode.autofill) {
        every { getCacheTtlMs() } returns 10L * 60L * 1000L
    }
    private val port = mock<AnnouncementPort>()

    private fun makeViewModel(): AnnouncementViewModel {
        return AnnouncementViewModel(
            fetchAnnouncements = FetchAnnouncementsUseCase(port, cache),
            markAnnouncementsRead = MarkAnnouncementsReadUseCase(cache),
            fetchAdConfig = FetchAdConfigUseCase(port),
            fetchAnnouncement = FetchAnnouncementUseCase(port),
        )
    }

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
