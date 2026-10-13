package restarhalf.stellar.schedule.data.local

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Announcement


class AnnouncementStore(
    private val settings: ObservableSettings,
) {
    
    fun getCacheTtlMs(): Long = CACHE_TTL_MS

    
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

    
    fun setCachedAnnouncements(announcements: List<Announcement>) {
        settings[KEY_CACHE] = Json.encodeToString(ListSerializer(Announcement.serializer()), announcements)
        settings[KEY_CACHE_AT_MS] = Clock.System.now().toEpochMilliseconds()
    }

    
    fun getCacheTimestampMs(): Long = settings.getLong(KEY_CACHE_AT_MS, 0L)

    
    fun getLastReadAtMs(): Long = settings.getLong(KEY_LAST_READ_AT_MS, 0L)

    
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
