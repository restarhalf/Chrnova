package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AnnouncementCachePort
import restarhalf.stellar.schedule.domain.model.Announcement


class MarkAnnouncementsReadUseCase(
    private val store: AnnouncementCachePort,
) {
    
    operator fun invoke(announcement: Announcement) {
        val targetMs = announcement.lastChangeAtMs
        if (targetMs > store.getLastReadAtMs()) {
            store.setLastReadAtMs(targetMs)
        }
    }
}
