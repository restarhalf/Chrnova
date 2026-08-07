package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlin.time.Clock
import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort

/**
 * 获取公告用例
 *
 * 负责公告的拉取与缓存策略：
 * - 缓存未过期（10 分钟）且非强制刷新时直接读缓存，避免每次进首页都打网络；
 * - 网络请求失败时降级用缓存；无缓存才向上抛错；
 * - 同时计算未读数量（按最后阅读时间与公告内容最后变化时间比较）。
 */
class FetchAnnouncementsUseCase(
    private val port: AnnouncementPort,
    private val store: AnnouncementStore,
) {
    /**
     * @param forceRefresh 强制忽略缓存重新拉取（下拉刷新）
     * @return 公告列表与未读数量
     */
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
                    // 网络失败：有缓存则降级展示，无缓存则抛错由 UI 提示
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
        /** 最后阅读时间（毫秒），UI 据此判断每条公告是否未读 */
        val lastReadAtMs: Long,
    )
}
