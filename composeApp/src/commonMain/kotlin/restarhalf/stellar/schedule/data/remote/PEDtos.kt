package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PELoginResponse(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
    @SerialName("token") val token: String? = null,
    @SerialName("user_id") val userId: String? = null,
)

@Serializable
data class PEScoreListResponse(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
    @SerialName("data_arr") val dataArr: List<PEYearScore> = emptyList(),
)

@Serializable
data class PEYearScore(
    @SerialName("school_year") val schoolYear: String,
    @SerialName("total") val total: Double,
    @SerialName("is_free") val isFree: Int,
    @SerialName("done") val done: Int,
    @SerialName("nums") val nums: Int,
)

@Serializable
data class PEDetailResponse(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
    @SerialName("data") val data: PEDetailData? = null,
)

@Serializable
data class PEDetailData(
    @SerialName("total_score") val totalScore: Double,
    @SerialName("total_grade") val totalGrade: String,
    @SerialName("data_arr") val dataArr: List<PESubjectScore> = emptyList(),
)

@Serializable
data class PESubjectScore(
    @SerialName("subject_id") val subjectId: String,
    @SerialName("sub_name") val subName: String,
    @SerialName("result") val result: String? = null,
    @SerialName("score") val score: Int? = null,
    @SerialName("unit") val unit: String,
    @SerialName("sub_ratio") val subRatio: String,
    @SerialName("grade") val grade: String? = null,
    @SerialName("is_join") val isJoin: Int,
)

@Serializable
data class PEStudentInfoResponse(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
    @SerialName("data") val data: PEStudentInfo? = null,
)

@Serializable
data class PEStudentInfo(
    @SerialName("testCode") val testCode: String,
    @SerialName("stuName") val stuName: String,
    @SerialName("stdNumber") val stdNumber: String,
)
