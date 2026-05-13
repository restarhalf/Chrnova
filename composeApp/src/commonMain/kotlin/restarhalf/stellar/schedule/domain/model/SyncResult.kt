package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncResult(
    val inserted: Int,
    val semesterId: String,
    val campusId: String,
    val campusName: String,
    val week: String,
)
