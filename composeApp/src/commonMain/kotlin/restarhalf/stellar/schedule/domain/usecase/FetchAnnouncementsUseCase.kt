package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlin.time.Clock
import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort

class FetchAnnouncementsUseCase(
    private val port: AnnouncementPort,
    private val store: AnnouncementCachePort,
) {

    suspend operator fun invoke(forceRefresh: Boolean = false): FetchResult {
        val cached = store.getCachedAnnouncements()
        val cacheFresh = cached != null &&
            Clock.System.now().toEpochMilliseconds() - store.getCacheTimestampMs() < store.getCacheTtlMs()

        val announcements: List<Announcement> =
            if (forceRefresh || cached == null || !cacheFresh) {
                try {
                    port.listAnnouncements().also { store.setCachedAnnouncements(it) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {

                    cached ?: throw e
                }
            } else {
                cached
            }

        val lastReadAtMs = store.getLastReadAtMs()
        val unreadCount = announcements.count { it.lastChangeAtMs > lastReadAtMs }
        return FetchResult(announcements, unreadCount, lastReadAtMs)
    }

    data class FetchResult(
        val announcements: List<Announcement>,
        val unreadCount: Int,

        val lastReadAtMs: Long,
    )
}
