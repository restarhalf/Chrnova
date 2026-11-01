package restarhalf.stellar.schedule.papers

private val PAPER_DOCUMENT_MIMES = setOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
)

fun isPaperDocument(attachment: PickedAttachment): Boolean {
    val extension = attachment.name.substringAfterLast('.', "").lowercase()
    if (extension == "pdf" || extension == "doc" || extension == "docx") return true
    return attachment.mime.lowercase() in PAPER_DOCUMENT_MIMES
}

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
