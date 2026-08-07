package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 公告数据模型
 *
 * 与后端 (chrnova.announcement.restarhalf.dpdns.org) 的 announcements 表对应。
 * 公开接口仅返回已发布（status=published）的公告。
 */
@Serializable
data class Announcement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    /** 优先级：0=普通，1=重要（列表页显示徽标） */
    val priority: Int = 0,
    /** 是否置顶（后端以 0/1 存储，API 输出布尔） */
    val pinned: Boolean = false,
    /** 发布时间（Unix 秒） */
    @SerialName("created_at") val createdAt: Long = 0,
    /** 更新时间（Unix 秒） */
    @SerialName("updated_at") val updatedAt: Long = 0,
) {
    val isImportant: Boolean get() = priority == 1

    /**
     * 内容最后变化时间（毫秒）：max(发布时间, 最近编辑时间)。
     * 未读判断（红点/未读数/已读推进）统一以此为准——编辑过的公告会重新变未读。
     */
    val lastChangeAtMs: Long get() = maxOf(createdAt, updatedAt) * 1000L
}
