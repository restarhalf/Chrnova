package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.AdConfig
import restarhalf.stellar.schedule.domain.port.AnnouncementPort

class FetchAdConfigUseCase(
    private val port: AnnouncementPort,
) {
    suspend operator fun invoke(): AdConfig? = port.getAdConfig()
}
