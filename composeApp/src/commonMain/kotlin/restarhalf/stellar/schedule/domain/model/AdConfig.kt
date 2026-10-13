package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AdConfig(
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val announcementId: String? = null,
)
