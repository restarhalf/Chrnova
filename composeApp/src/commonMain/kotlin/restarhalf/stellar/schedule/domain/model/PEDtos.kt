package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable




@Immutable
@Serializable
data class PELoginResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("token") val token: String? = null,
    @SerialName("user_id") val userId: String? = null,
)


@Immutable
@Serializable
data class PEScoreListResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data_arr") val dataArr: List<PEYearScore> = emptyList(),
)


@Immutable
@Serializable
data class PEYearScore(
    @SerialName("school_year") val schoolYear: String = "",
    @SerialName("total") val total: Double = 0.0,
    @SerialName("is_free") val isFree: Int = 0,
    @SerialName("done") val done: Int = 0,
    @SerialName("nums") val nums: Int = 0,
)


@Immutable
@Serializable
data class PEDetailResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEDetailData? = null,
)


@Immutable
@Serializable
data class PEDetailData(
    @SerialName("total_score") val totalScore: Double = 0.0,
    @SerialName("total_grade") val totalGrade: String = "",
    @SerialName("data_arr") val dataArr: List<PESubjectScore> = emptyList(),
)


@Immutable
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


@Immutable
@Serializable
data class PESubjectHistoryResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PESubjectHistoryData? = null,
)


@Immutable
@Serializable
data class PESubjectHistoryData(
    @SerialName("data_list") val dataList: List<PESubjectHistoryItem> = emptyList(),
    @SerialName("total_rows") val totalRows: Int = 0,
)


@Immutable
@Serializable
data class PESubjectHistoryItem(
    @SerialName("result") val result: String? = null,
    @SerialName("session_name") val sessionName: String = "",
    @SerialName("score_time") val scoreTime: String = "",
    @SerialName("sourceScoreId") val sourceScoreId: String = "",
    @SerialName("sub_name") val subName: String = "",
    @SerialName("score_status") val scoreStatus: String = "",
)


@Immutable
@Serializable
data class PEAuthProfileResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEAuthProfile? = null,
)


@Immutable
@Serializable
data class PEAppointmentListResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEAppointmentListData? = null,
)


@Immutable
@Serializable
data class PEAppointmentListData(
    @SerialName("data_list") val dataList: List<PEAppointmentItem> = emptyList(),
    
    @SerialName("total_rows") val totalRows: String = "0",
) {
    val totalRowsInt: Int
        get() = totalRows.toIntOrNull() ?: dataList.size
}


@Immutable
@Serializable
data class PEAppointmentItem(
    @SerialName("appointment_id") val appointmentId: String = "",
    
    @SerialName("temporary_id") val temporaryId: String = "",
    @SerialName("appointment_name") val appointmentName: String = "",
    
    @SerialName("appointment_status") val appointmentStatus: String = "",
    
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


@Immutable
@Serializable
data class PEAppointmentDetailResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEAppointmentDetail? = null,
)


@Immutable
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
    
    @SerialName("tern_list") val availableDates: List<String> = emptyList(),
    @SerialName("times_list") val timesList: List<PEAppointmentTimesBrief> = emptyList(),
)


@Immutable
@Serializable
data class PEAppointmentTimesBrief(
    @SerialName("appointment_times") val appointmentTimes: String = "",
    @SerialName("already_quota") val alreadyQuota: Int = 0,
    @SerialName("time_quota") val timeQuota: Int = 0,
)


@Immutable
@Serializable
data class PEAppointmentTimesResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("dataList") val dataList: List<PEAppointmentTimeSlot> = emptyList(),
)


@Immutable
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


@Immutable
@Serializable
data class PEAppointmentActionResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
)


@Immutable
@Serializable
data class PEFreeApplyListResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: List<PEFreeApplyItem> = emptyList(),
)


@Immutable
@Serializable
data class PEFreeApplyItem(
    @SerialName("applyId") val applyId: String = "",
    @SerialName("schoolYear") val schoolYear: String = "",
    @SerialName("freeApplyType") val freeApplyType: String = "",
    @SerialName("applyStatus") val applyStatus: String = "",
    @SerialName("stdNumber") val stdNumber: String = "",
    @SerialName("attachments") val attachments: String = "",
    @SerialName("applyTime") val applyTime: String = "",
    @SerialName("remark") val remark: String = "",
    @SerialName("crtTime") val crtTime: String = "",
    @SerialName("crtUsr") val crtUsr: String = "",
    @SerialName("stuName") val stuName: String = "",
    @SerialName("schoolGrade") val schoolGrade: String = "",
    @SerialName("attList") val attList: List<PEFreeApplyAtt> = emptyList(),
) {
    
    val displayTime: String get() = applyTime.ifBlank { crtTime }
}


@Immutable
@Serializable
data class PEFreeApplyAtt(
    @SerialName("attId") val attId: String = "",
    @SerialName("fileName") val fileName: String = "",
)


@Immutable
@Serializable
data class PEFreeApplyDetailResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEFreeApplyItem? = null,
)


@Immutable
@Serializable
data class PEFreeSchoolYearResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PEFreeSchoolYearData? = null,
)

@Immutable
@Serializable
data class PEFreeSchoolYearData(
    @SerialName("nowSchoolYear") val nowSchoolYear: String = "",
    @SerialName("dataList") val dataList: List<PECodeItem> = emptyList(),
)

@Immutable
@Serializable
data class PECodeItem(
    @SerialName("code") val code: String = "",
    @SerialName("label") val label: String = "",
)


@Immutable
@Serializable
data class PEFreeActionResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("att_id") val attId: String? = null,
)


@Immutable
@Serializable
data class PECodeListResponse(
    @SerialName("status") val status: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("data") val data: PECodeListData? = null,
)

@Immutable
@Serializable
data class PECodeListData(
    @SerialName("free_apply_type") val freeApplyType: List<PECodeItem> = emptyList(),
    @SerialName("gym_free_apply_status") val gymFreeApplyStatus: List<PECodeItem> = emptyList(),
    @SerialName("dataList") val dataList: List<PECodeItem> = emptyList(),
    @SerialName("data_list") val dataListSnake: List<PECodeItem> = emptyList(),
)
