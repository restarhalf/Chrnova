package restarhalf.stellar.schedule.domain.model

data class SyncResult(
    val inserted: Int,
    val semesterId: String,
    val campusId: String,
    val campusName: String,
    val week: String,
)
