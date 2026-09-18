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

/** 学生预约列表响应 */
@Serializable
data class PEAppointmentListResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEAppointmentListData? = null,
)

/** 学生预约列表数据 */
@Serializable
data class PEAppointmentListData(
    @SerialName("data_list") val dataList: List<PEAppointmentItem> = emptyList(),
    /** 服务端返回字符串，如 "4" */
    @SerialName("total_rows") val totalRows: String = "0",
) {
    val totalRowsInt: Int
        get() = totalRows.toIntOrNull() ?: dataList.size
}

/** 预约条目（我的预约 / 可预约共用；可预约侧以 appointment_id 为主键） */
@Serializable
data class PEAppointmentItem(
    @SerialName("appointment_id") val appointmentId: String = "",
    /** 仅「我的预约」可能有，用于 cancelRegistration */
    @SerialName("temporary_id") val temporaryId: String = "",
    @SerialName("appointment_name") val appointmentName: String = "",
    /** 0 已取消 / 1 已预约 / 2 已取消 / 3 可预约 / 4 已结束/不可约 */
    @SerialName("appointment_status") val appointmentStatus: String = "",
    /** 可预约侧为日期区间字符串；已预约侧可能是具体日期 */
    @SerialName("appointment_date") val appointmentDate: String = "",
    @SerialName("appointment_times") val appointmentTimes: String = "",
    @SerialName("enter_start_time") val enterStartTime: String = "",
    @SerialName("enter_end_time") val enterEndTime: String = "",
    @SerialName("crt_time") val crtTime: String = "",
    @SerialName("already_quota") val alreadyQuota: Int = 0,
    @SerialName("time_quota") val timeQuota: Int = 0,
    @SerialName("max_app_num") val maxAppNum: Int = 0,
    @SerialName("appointment_content") val appointmentContent: String = "",
)

/** 预约详情响应（selectUserAppointmentDetail） */
@Serializable
data class PEAppointmentDetailResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEAppointmentDetail? = null,
)

/** 预约详情 */
@Serializable
data class PEAppointmentDetail(
    @SerialName("appointment_id") val appointmentId: String = "",
    @SerialName("appointment_name") val appointmentName: String = "",
    @SerialName("appointment_status") val appointmentStatus: String = "",
    @SerialName("appointment_date") val appointmentDate: String = "",
    @SerialName("appointment_content") val appointmentContent: String = "",
    @SerialName("enter_start_time") val enterStartTime: String = "",
    @SerialName("enter_end_time") val enterEndTime: String = "",
    @SerialName("already_quota") val alreadyQuota: Int = 0,
    @SerialName("time_quota") val timeQuota: Int = 0,
    @SerialName("max_app_num") val maxAppNum: Int = 0,
    /** 可选日期列表（字段名服务端为 tern_list） */
    @SerialName("tern_list") val availableDates: List<String> = emptyList(),
    @SerialName("times_list") val timesList: List<PEAppointmentTimesBrief> = emptyList(),
)

/** 详情中的时段摘要（无 times_id，正式约需再查 selectAppointmentTimes） */
@Serializable
data class PEAppointmentTimesBrief(
    @SerialName("appointment_times") val appointmentTimes: String = "",
    @SerialName("already_quota") val alreadyQuota: Int = 0,
    @SerialName("time_quota") val timeQuota: Int = 0,
)

/** 某日可约时段响应（selectAppointmentTimes） */
@Serializable
data class PEAppointmentTimesResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("dataList") val dataList: List<PEAppointmentTimeSlot> = emptyList(),
)

/** 某日可约时段 */
@Serializable
data class PEAppointmentTimeSlot(
    @SerialName("times_id") val timesId: String = "",
    @SerialName("appointment_id") val appointmentId: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    @SerialName("enters") val enters: Int = 0,
    @SerialName("quota") val quota: Int = 0,
    @SerialName("isEntered") val isEntered: String = "0",
) {
    val label: String get() = "$startTime~$endTime"
}

/** 预约操作（取消 / 报名）响应 */
@Serializable
data class PEAppointmentActionResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
)
