package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Paper(
    val id: String = "",
    val title: String = "",
    val folder: String = "",
    val path: String = "",
    val size: Long = 0,
    @SerialName("device_id") val deviceId: String = "",
    val downloads: Int = 0,
    @SerialName("created_at") val createdAt: Long = 0,
)
