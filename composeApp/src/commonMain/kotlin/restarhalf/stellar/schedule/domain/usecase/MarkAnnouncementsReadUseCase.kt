package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.local.AnnouncementStore
import restarhalf.stellar.schedule.domain.model.Announcement


class MarkAnnouncementsReadUseCase(
    private val store: AnnouncementStore,
) {
    
    operator fun invoke(announcement: Announcement) {
        val targetMs = announcement.lastChangeAtMs
        if (targetMs > store.getLastReadAtMs()) {
            store.setLastReadAtMs(targetMs)
        }
    }
}
