package restarhalf.stellar.schedule.pictureselector

data class MediaImage(
    val id: Long,
    val contentUri: String,
    val dateTakenMs: Long,
    val bucketId: Long,
    val bucketName: String,
)

data class MediaAlbum(
    val bucketId: Long,
    val bucketName: String,
    val coverUri: String,
    val count: Int,
)