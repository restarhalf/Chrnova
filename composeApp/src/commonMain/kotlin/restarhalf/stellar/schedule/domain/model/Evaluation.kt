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
    /** 用户 hash（学号 SHA-256，由后端按 X-User-Hash 头计算） */
    @SerialName("user_hash") val userHash: String = "",
    /** 点赞数 */
    val likes: Int = 0,
    /** 状态：后端已去掉审核流程，新建评价一律 approved。字段保留做向前兼容。 */
    val status: String = "approved",
    @SerialName("created_at") val createdAt: Long = 0,
    /** 当前用户是否已点赞（由后端按 X-User-Hash 计算） */
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
)

/**
 * 编辑评价请求体（仅本人可改；course_name 不可改）。
 *
 * 字段为 null 表示不修改该字段。
 */
@Serializable
data class EvaluationUpdateRequest(
    val teacher: String? = null,
    val rating: Int? = null,
    val content: String? = null,
    val anonymous: Boolean? = null,
    val author: String? = null,
)

/** 点赞操作返回结果 */
@Serializable
data class LikeResult(
    val likes: Int = 0,
    val liked: Boolean = false,
)

/**
 * 课程评价聚合摘要（按课程名聚合）。
 *
 * 与后端 GET /courses 接口返回一一对应，用于评价列表页的课程卡片视图。
 */
@Serializable
data class CourseEvaluationSummary(
    @SerialName("course_name") val courseName: String = "",
    /** 教师名（同一课程多名教师时取其一） */
    val teacher: String = "",
    /** 平均评分（1-5，保留 1 位小数） */
    @SerialName("avg_rating") val avgRating: Double = 0.0,
    /** 评价总数 */
    @SerialName("eval_count") val evalCount: Int = 0,
    /** 最新评价时间（Unix 秒） */
    @SerialName("latest_at") val latestAt: Long = 0,
)
