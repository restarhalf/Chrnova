package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 课程评价数据模型
 *
 * 与后端 (chrnova.evaluate.restarhalf.dpdns.org) 的 evaluations 表一一对应。
 */
@Serializable
data class Evaluation(
    val id: String = "",
    @SerialName("course_name") val courseName: String = "",
    val teacher: String = "",
    /** 评分 1-5 */
    val rating: Int = 0,
    val content: String = "",
    /** 是否匿名 */
    val anonymous: Boolean = false,
    /** 作者名（匿名时为空） */
    val author: String = "",
    @SerialName("user_no") val userNo: String = "",
    @SerialName("device_id") val deviceId: String = "",
    /** 点赞数 */
    val likes: Int = 0,
    /** 审核状态：pending / approved / rejected */
    val status: String = "pending",
    @SerialName("created_at") val createdAt: Long = 0,
    /** 当前设备是否已点赞（由后端按 X-Device-Id 计算） */
    val liked: Boolean = false,
)

/** 分页评价列表 */
@Serializable
data class EvaluationPage(
    val items: List<Evaluation> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20,
)

/** 提交评价请求体 */
@Serializable
data class EvaluationCreateRequest(
    @SerialName("course_name") val courseName: String,
    val teacher: String = "",
    val rating: Int,
    val content: String,
    val anonymous: Boolean = false,
    val author: String = "",
    @SerialName("user_no") val userNo: String = "",
)

/** 点赞操作返回结果 */
@Serializable
data class LikeResult(
    val likes: Int = 0,
    val liked: Boolean = false,
)
