package restarhalf.stellar.schedule.ui.model

import androidx.compose.runtime.Stable

@Stable
data class AttachmentUi(
    val attId: String,
    val name: String,
    val mimeType: String,
    val previewBytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AttachmentUi) return false
        return attId == other.attId && name == other.name
    }

    override fun hashCode(): Int = attId.hashCode() * 31 + name.hashCode()
}
