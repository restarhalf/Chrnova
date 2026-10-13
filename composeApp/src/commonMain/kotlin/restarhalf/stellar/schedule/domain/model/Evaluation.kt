package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Evaluation(
    val id: String = "",
    @SerialName("course_name") val courseName: String = "",
    val teacher: String = "",
    val rating: Int = 0,
    val content: String = "",
    val anonymous: Boolean = false,
    val author: String = "",
    @SerialName("user_hash") val userHash: String = "",
    val likes: Int = 0,
    val status: String = "approved",
    @SerialName("created_at") val createdAt: Long = 0,
    val liked: Boolean = false,
)

@Serializable
data class EvaluationPage(
    val items: List<Evaluation> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val size: Int = 20,
)

@Serializable
data class EvaluationCreateRequest(
    @SerialName("course_name") val courseName: String,
    val teacher: String = "",
    val rating: Int,
    val content: String,
    val anonymous: Boolean = false,
    val author: String = "",
)

@Serializable
data class EvaluationUpdateRequest(
    val teacher: String? = null,
    val rating: Int? = null,
    val content: String? = null,
    val anonymous: Boolean? = null,
    val author: String? = null,
)

@Serializable
data class LikeResult(
    val likes: Int = 0,
    val liked: Boolean = false,
)

@Serializable
data class CourseEvaluationSummary(
    @SerialName("course_name") val courseName: String = "",
    val teacher: String = "",
    @SerialName("avg_rating") val avgRating: Double = 0.0,
    @SerialName("eval_count") val evalCount: Int = 0,
    @SerialName("latest_at") val latestAt: Long = 0,
)
