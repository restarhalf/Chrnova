package restarhalf.stellar.schedule.data.remote

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

    suspend fun getTeachingWeek(): JwxtTeachingWeekResponse

    suspend fun getCampusList(): JwxtCampusResponse

    suspend fun fetchCurriculum(fields: Map<String, String>): JwxtCurriculumResponse

    suspend fun fetchExaminationArrangement(
        semester: String = "",
        nameOrNumber: String = ""
    ): JwxtExaminationResponse

    suspend fun fetchTermGradeReport(semester: String): JwxtTermGradeResponse
}
