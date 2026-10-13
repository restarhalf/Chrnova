package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Announcement
import restarhalf.stellar.schedule.domain.port.AnnouncementPort

class FetchAnnouncementUseCase(
    private val port: AnnouncementPort,
) {
    suspend operator fun invoke(id: String): Announcement = port.getAnnouncement(id)
}
