package restarhalf.stellar.schedule.papers

data class PickedAttachment(
    val bytes: ByteArray,
    val name: String,
    val mime: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedAttachment) return false
        return name == other.name && mime == other.mime && bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mime.hashCode()
        return result
    }
}
