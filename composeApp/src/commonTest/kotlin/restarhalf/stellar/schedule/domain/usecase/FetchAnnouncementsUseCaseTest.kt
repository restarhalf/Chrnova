package restarhalf.stellar.schedule.domain.usecase

import com.russhwolf.settings.ObservableSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock


class FetchAnnouncementsUseCaseTest {

    private val settings = mock<ObservableSettings>(MockMode.autofill) {
        every { putString(any(), any()) } returns Unit
        every { putLong(any(), any()) } returns Unit
    }
    private val store = AnnouncementStore(settings)
    private val port = mock<AnnouncementPort>()
    private val useCase = FetchAnnouncementsUseCase(port, store)

    private fun cachedJson(announcements: List<Announcement>): String =
        Json.encodeToString(announcements)

    @Test
    fun `缓存新鲜时直接返回缓存且不打网络`() = runTest {
        val cached = listOf(
            Announcement(id = "1", title = "缓存公告", createdAt = 1000L, updatedAt = 0L),
        )
        every { settings.getStringOrNull(any()) } returns cachedJson(cached)
        every { settings.getLong(any(), any()) } returns Clock.System.now().toEpochMilliseconds()

        val result = useCase()

        assertEquals(listOf("缓存公告"), result.announcements.map { it.title })
        verifySuspend(VerifyMode.not) { port.listAnnouncements() }
    }

    @Test
    fun `缓存过期时重新拉取并回写缓存`() = runTest {
        val cached = listOf(Announcement(id = "1", title = "旧缓存", createdAt = 1000L))
        val fresh = listOf(Announcement(id = "2", title = "新公告", createdAt = 2000L))
        every { settings.getStringOrNull(any()) } returns cachedJson(cached)

        every { settings.getLong(any(), any()) } returns 0L
        everySuspend { port.listAnnouncements() } returns fresh

        val result = useCase()

        assertEquals(listOf("新公告"), result.announcements.map { it.title })
        verifySuspend { port.listAnnouncements() }

        verify { settings.putString(any(), any()) }
        verify { settings.putLong(any(), any()) }
    }

    @Test
    fun `强制刷新时忽略新鲜缓存`() = runTest {
        val fresh = listOf(Announcement(id = "2", title = "强制刷新结果", createdAt = 2000L))
        every { settings.getLong(any(), any()) } returns Clock.System.now().toEpochMilliseconds()
        everySuspend { port.listAnnouncements() } returns fresh

        val result = useCase(forceRefresh = true)

        assertEquals(listOf("强制刷新结果"), result.announcements.map { it.title })
        verifySuspend { port.listAnnouncements() }
    }

    @Test
    fun `网络失败时降级返回缓存`() = runTest {
        val cached = listOf(Announcement(id = "1", title = "降级缓存", createdAt = 1000L))
        every { settings.getStringOrNull(any()) } returns cachedJson(cached)
        every { settings.getLong(any(), any()) } returns 0L // 过期，强制走网络
        everySuspend { port.listAnnouncements() } throws RuntimeException("网络异常")

        val result = useCase()

        assertEquals(listOf("降级缓存"), result.announcements.map { it.title })
    }

    @Test
    fun `网络失败且无缓存时向上抛错`() = runTest {

        everySuspend { port.listAnnouncements() } throws IllegalStateException("后端不可用")

        assertFailsWith<IllegalStateException> { useCase() }
    }

    @Test
    fun `未读数量按内容变化时间计算`() = runTest {
        val nowSec = Clock.System.now().toEpochMilliseconds() / 1000L
        val readAnnouncement = Announcement(id = "1", title = "已读", createdAt = nowSec - 3600)
        val unreadAnnouncement = Announcement(id = "2", title = "未读", createdAt = nowSec + 10)
        everySuspend { port.listAnnouncements() } returns listOf(readAnnouncement, unreadAnnouncement)


        val result = useCase()

        assertEquals(0L, result.lastReadAtMs)
        assertEquals(2, result.unreadCount)
    }
}
