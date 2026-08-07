package restarhalf.stellar.schedule.data.local

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Announcement

/**
 * 公告本地缓存与已读状态存储
 *
 * 公告是低频小数据，用 multiplatform-settings 存 JSON，避免引入 Room 迁移。
 * 缓存命中策略由 [FetchAnnouncementsUseCase] 决定，本类只负责读写。
 *
 * @param settings 共享设置实例（schedule_settings）
 */
class AnnouncementStore(
    private val settings: ObservableSettings,
) {
    /** 缓存有效期：10 分钟 */
    fun getCacheTtlMs(): Long = CACHE_TTL_MS

    /**
     * 读取缓存的公告列表。
     *
     * @return 缓存不存在或解析失败时返回 null
     */
    fun getCachedAnnouncements(): List<Announcement>? {
        val raw = settings.getStringOrNull(KEY_CACHE)
        if (raw.isNullOrBlank()) return null
        return try {
            Json.decodeFromString(ListSerializer(Announcement.serializer()), raw)
        } catch (e: Exception) {
            AppLogger.log("Announcement", "解析公告缓存失败", e)
            null
        }
    }

    /** 写入缓存（含时间戳） */
    fun setCachedAnnouncements(announcements: List<Announcement>) {
        settings[KEY_CACHE] = Json.encodeToString(ListSerializer(Announcement.serializer()), announcements)
        settings[KEY_CACHE_AT_MS] = Clock.System.now().toEpochMilliseconds()
    }

    /** 上次缓存写入时间（毫秒）；从未缓存返回 0 */
    fun getCacheTimestampMs(): Long = settings.getLong(KEY_CACHE_AT_MS, 0L)

    /** 最后阅读时间（毫秒）；从未阅读返回 0 */
    fun getLastReadAtMs(): Long = settings.getLong(KEY_LAST_READ_AT_MS, 0L)

    /** 记录阅读时间（毫秒） */
    fun setLastReadAtMs(ms: Long) {
        settings[KEY_LAST_READ_AT_MS] = ms
    }

    private companion object {
        private const val KEY_CACHE = "announcement_cache"
        private const val KEY_CACHE_AT_MS = "announcement_cache_at_ms"
        private const val KEY_LAST_READ_AT_MS = "announcement_last_read_at_ms"
        private const val CACHE_TTL_MS = 10L * 60L * 1000L
    }
}
