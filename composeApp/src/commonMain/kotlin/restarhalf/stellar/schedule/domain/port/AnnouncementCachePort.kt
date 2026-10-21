package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Announcement


interface AnnouncementCachePort {

    fun getCachedAnnouncements(): List<Announcement>?


    fun setCachedAnnouncements(announcements: List<Announcement>)


    fun getCacheTimestampMs(): Long


    fun getCacheTtlMs(): Long


    fun getLastReadAtMs(): Long


    fun setLastReadAtMs(ms: Long)
}
