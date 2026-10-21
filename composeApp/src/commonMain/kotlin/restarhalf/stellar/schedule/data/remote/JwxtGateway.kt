package restarhalf.stellar.schedule.data.remote

import restarhalf.stellar.schedule.domain.model.JwxtSelectionOperResult

interface JwxtGateway {
    
    suspend fun login(
        userNo: String,
        password: String,
        captchaData: String = "",
        codeVal: String = "",
        p: String? = null
    ): JwxtLoginResponse

    
    suspend fun getCurrentTerm(): JwxtCurrentTermResponse

    
    suspend fun getSemesterList(): List<JwxtSemesterItem>

    
    suspend fun getSemesterListFromEndpoint(): List<JwxtSemesterListItem>

    
    suspend fun getTeachingWeek(): JwxtTeachingWeekResponse

    
    suspend fun getCampusList(): JwxtCampusResponse

    
    suspend fun fetchCurriculum(fields: Map<String, String>): JwxtCurriculumResponse

    
    suspend fun fetchExaminationArrangement(
        semester: String = "",
        nameOrNumber: String = ""
    ): JwxtExaminationResponse

    
    suspend fun fetchTermGradeReport(semester: String): JwxtTermGradeResponse

    
    suspend fun fetchGuidanceTeachingCourses(
        kcxz: String,
        kcsx: String = "",
        kcmc: String = ""
    ): JwxtGuidanceTeachingResponse



    
    suspend fun fetchSelectionRotations(isnew: Int = 1): JwxtSelectionResponse

    
    suspend fun initSelectionSession(rotationId: String): JwxtSelectionResponse

    
    suspend fun fetchSelectionCourses(
        rotationId: String,
        classificationCode: String,
        sessionTime: String,
        extraRules: Map<String, String> = emptyMap(),
        courseInformation: String = "",
    ): JwxtSelectionResponse

    
    suspend fun submitSelection(
        rotationId: String,
        courseId: String,
        noticeId: String,
        sessionTime: String,
        classificationCode: String,
        splitIdentification: String = "",
        selectedNoticeId: String = "",
        selectedSplitIdentification: String = "",
        extraRules: Map<String, String> = emptyMap(),
    ): JwxtSelectionOperResult

    
    suspend fun dropSelection(
        rotationId: String,
        noticeId: String,
        sessionTime: String,
        courseQualification: String = "true",
    ): JwxtSelectionResponse

    
    suspend fun fetchSelectedCourses(rotationId: String): JwxtSelectionResponse
}
