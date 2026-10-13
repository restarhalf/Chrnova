package restarhalf.stellar.schedule.data.remote


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
