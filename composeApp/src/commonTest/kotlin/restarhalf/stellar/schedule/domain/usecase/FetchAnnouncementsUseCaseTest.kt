package restarhalf.stellar.schedule.domain.usecase

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
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
import restarhalf.stellar.schedule.domain.port.AnnouncementPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Clock

class FetchAnnouncementsUseCaseTest {

    private val store = mock<AnnouncementCachePort>(MockMode.autofill) {
        every { getCacheTtlMs() } returns CACHE_TTL_MS
    }
    private val port = mock<AnnouncementPort>()
    private val useCase = FetchAnnouncementsUseCase(port, store)

    private fun cached(vararg announcements: Announcement) {
        every { store.getCachedAnnouncements() } returns announcements.toList()
    }

    private fun cacheWrittenAt(ms: Long) {
        every { store.getCacheTimestampMs() } returns ms
    }

    @Test
    fun `缓存新鲜时直接返回缓存且不打网络`() = runTest {
        cached(Announcement(id = "1", title = "缓存公告", createdAt = 1000L, updatedAt = 0L))
        cacheWrittenAt(Clock.System.now().toEpochMilliseconds())

        val result = useCase()

        assertEquals(listOf("缓存公告"), result.announcements.map { it.title })
        verifySuspend(VerifyMode.not) { port.listAnnouncements() }
    }

    @Test
    fun `缓存过期时重新拉取并回写缓存`() = runTest {
        val fresh = listOf(Announcement(id = "2", title = "新公告", createdAt = 2000L))
        cached(Announcement(id = "1", title = "旧缓存", createdAt = 1000L))
        cacheWrittenAt(0L)
        everySuspend { port.listAnnouncements() } returns fresh

        val result = useCase()

        assertEquals(listOf("新公告"), result.announcements.map { it.title })
        verifySuspend { port.listAnnouncements() }
        verify { store.setCachedAnnouncements(fresh) }
    }

    @Test
    fun `强制刷新时忽略新鲜缓存`() = runTest {
        val fresh = listOf(Announcement(id = "2", title = "强制刷新结果", createdAt = 2000L))
        cacheWrittenAt(Clock.System.now().toEpochMilliseconds())
        everySuspend { port.listAnnouncements() } returns fresh

        val result = useCase(forceRefresh = true)

        assertEquals(listOf("强制刷新结果"), result.announcements.map { it.title })
        verifySuspend { port.listAnnouncements() }
    }

    @Test
    fun `网络失败时降级返回缓存`() = runTest {
        cached(Announcement(id = "1", title = "降级缓存", createdAt = 1000L))
        cacheWrittenAt(0L)
        everySuspend { port.listAnnouncements() } throws RuntimeException("网络异常")

        val result = useCase()

        assertEquals(listOf("降级缓存"), result.announcements.map { it.title })
    }

    @Test
    fun `网络失败且无缓存时向上抛错`() = runTest {
        every { store.getCachedAnnouncements() } returns null
        cacheWrittenAt(0L)
        everySuspend { port.listAnnouncements() } throws IllegalStateException("后端不可用")

        assertFailsWith<IllegalStateException> { useCase() }
    }

    @Test
    fun `未读数量按内容变化时间计算`() = runTest {
        val nowSec = Clock.System.now().toEpochMilliseconds() / 1000L
        val readAnnouncement = Announcement(id = "1", title = "已读", createdAt = nowSec - 3600)
        val unreadAnnouncement = Announcement(id = "2", title = "未读", createdAt = nowSec + 10)
        everySuspend { port.listAnnouncements() } returns listOf(readAnnouncement, unreadAnnouncement)
        every { store.getLastReadAtMs() } returns 0L

        val result = useCase()

        assertEquals(0L, result.lastReadAtMs)
        assertEquals(2, result.unreadCount)
    }

    private companion object {
        const val CACHE_TTL_MS = 10L * 60L * 1000L
    }
}
