package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.AdConfig
import restarhalf.stellar.schedule.domain.model.Announcement

interface AnnouncementPort {
    suspend fun listAnnouncements(limit: Int = 50): List<Announcement>

    suspend fun getAnnouncement(id: String): Announcement

    suspend fun getAdConfig(): AdConfig?
}
