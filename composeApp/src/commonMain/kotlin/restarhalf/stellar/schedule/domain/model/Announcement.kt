package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    
    val priority: Int = 0,
    
    val pinned: Boolean = false,
    
    @SerialName("created_at") val createdAt: Long = 0,
    
    @SerialName("updated_at") val updatedAt: Long = 0,
) {
    val isImportant: Boolean get() = priority == 1

    val lastChangeAtMs: Long get() = maxOf(createdAt, updatedAt) * 1000L
}
