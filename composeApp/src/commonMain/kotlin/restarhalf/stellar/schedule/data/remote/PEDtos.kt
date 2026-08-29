package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 体育系统API数据传输对象（DTO）
 *
 * 定义与体育系统通信的所有数据结构，用于JSON序列化/反序列化。
 */

/** 登录响应 */
@Serializable
data class PELoginResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("token") val token: String? = null,
    @SerialName("user_id") val userId: String? = null,
)

/** 成绩列表响应 */
@Serializable
data class PEScoreListResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data_arr") val dataArr: List<PEYearScore> = emptyList(),
)

/** 年度成绩 */
@Serializable
data class PEYearScore(
    @SerialName("school_year") val schoolYear: String = "",
    @SerialName("total") val total: Double = 0.0,
    @SerialName("is_free") val isFree: Int = 0,
    @SerialName("done") val done: Int = 0,
    @SerialName("nums") val nums: Int = 0,
)

/** 成绩详情响应 */
@Serializable
data class PEDetailResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEDetailData? = null,
)

/** 成绩详情数据 */
@Serializable
data class PEDetailData(
    @SerialName("total_score") val totalScore: Double = 0.0,
    @SerialName("total_grade") val totalGrade: String = "",
    @SerialName("data_arr") val dataArr: List<PESubjectScore> = emptyList(),
)

/** 科目成绩 */
@Serializable
data class PESubjectScore(
    @SerialName("subject_id") val subjectId: String = "",
    @SerialName("sub_name") val subName: String = "",
    @SerialName("result") val result: String? = null,
    @SerialName("score") val score: Int? = null,
    @SerialName("unit") val unit: String = "",
    @SerialName("sub_ratio") val subRatio: String = "",
    @SerialName("grade") val grade: String? = null,
    @SerialName("is_join") val isJoin: Int = 0,
)

/** 单科成绩历史响应 */
@Serializable
data class PESubjectHistoryResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PESubjectHistoryData? = null,
)

/** 单科成绩历史数据 */
@Serializable
data class PESubjectHistoryData(
    @SerialName("data_list") val dataList: List<PESubjectHistoryItem> = emptyList(),
    @SerialName("total_rows") val totalRows: Int = 0,
)

/** 单科成绩历史记录 */
@Serializable
data class PESubjectHistoryItem(
    @SerialName("result") val result: String? = null,
    @SerialName("session_name") val sessionName: String = "",
    @SerialName("score_time") val scoreTime: String = "",
    @SerialName("sourceScoreId") val sourceScoreId: String = "",
    @SerialName("sub_name") val subName: String = "",
    @SerialName("score_status") val scoreStatus: String = "",
)

/** 学生信息响应 */
@Serializable
data class PEAuthProfileResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEAuthProfile? = null,
)

/** 学生信息 */
@Serializable
data class PEAuthProfile(
    @SerialName("testCode") val testCode: String = "",
    @SerialName("stuName") val stuName: String = "",
    @SerialName("stdNumber") val stdNumber: String = "",
)
