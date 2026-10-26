package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.PEAppointmentActionResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentDetailResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentListResponse
import restarhalf.stellar.schedule.domain.model.PEAppointmentTimesResponse
import restarhalf.stellar.schedule.domain.model.PEAuthProfileResponse
import restarhalf.stellar.schedule.domain.model.PEDetailResponse
import restarhalf.stellar.schedule.domain.model.PEFreeActionResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyDetailResponse
import restarhalf.stellar.schedule.domain.model.PEFreeApplyListResponse
import restarhalf.stellar.schedule.domain.model.PEFreeSchoolYearResponse
import restarhalf.stellar.schedule.domain.model.PELoginResponse
import restarhalf.stellar.schedule.domain.model.PEScoreListResponse
import restarhalf.stellar.schedule.domain.model.PESubjectHistoryResponse

interface PEGateway {

    suspend fun login(username: String, password: String): PELoginResponse

    suspend fun getScoreList(): PEScoreListResponse

    suspend fun getScoreDetail(schoolYear: String): PEDetailResponse

    suspend fun getSubjectScoreHistory(
        schoolYear: String,
        subjectId: String,
        pageNum: Int = 1,
        pageSize: Int = 50,
    ): PESubjectHistoryResponse

    suspend fun getProfile(): PEAuthProfileResponse

    suspend fun getAppointments(
        type: String,
        pageNum: Int = 1,
        pageSize: Int = 20,
    ): PEAppointmentListResponse

    suspend fun getAppointmentDetail(
        appointmentId: String,
        appointmentStatus: String,
    ): PEAppointmentDetailResponse

    suspend fun getAppointmentTimes(
        appointmentId: String,
        appointmentDate: String,
    ): PEAppointmentTimesResponse

    suspend fun cancelAppointment(temporaryId: String): PEAppointmentActionResponse

    suspend fun enterAppointment(
        appointmentId: String,
        timesId: String,
        enterDate: String,
    ): PEAppointmentActionResponse

    suspend fun getFreeApplyList(): PEFreeApplyListResponse

    suspend fun getFreeApplyDetail(applyId: String): PEFreeApplyDetailResponse

    suspend fun getFreeSchoolYears(): PEFreeSchoolYearResponse

    suspend fun submitFreeApply(
        stdNumber: String,
        schoolYear: String,
        freeApplyType: String,
        attachments: List<String>,
    ): PEFreeActionResponse

    suspend fun uploadPeFile(
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
    ): PEFreeActionResponse

    suspend fun downloadPeFile(attId: String): ByteArray

    fun peFileDownloadUrl(attId: String): String
}
