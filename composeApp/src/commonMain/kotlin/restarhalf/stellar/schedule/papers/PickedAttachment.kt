package restarhalf.stellar.schedule.papers

/** 文件选择结果 */
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
