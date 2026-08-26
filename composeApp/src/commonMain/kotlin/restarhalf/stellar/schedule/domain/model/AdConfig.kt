package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.Serializable

/**
 * 广告位配置（公告列表页顶部横幅），由公告 Worker 的 GET /ad 接口下发。
 *
 * 三个字段均可空：后端仅在启用且至少一项非空时返回对象；否则返回 null，
 * 客户端据此隐藏广告位。字段命名与后端一致（camelCase），反序列化无需 @SerialName。
 */
@Serializable
data class AdConfig(
    val imageUrl: String? = null,
    val targetUrl: String? = null,
    val announcementId: String? = null,
)
