package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.RemoteCampus
import restarhalf.stellar.schedule.domain.model.TermGradeReport

interface AcademicPort {
    suspend fun fetchCurrentTermId(): String
    suspend fun fetchCampuses(): List<RemoteCampus>
    suspend fun fetchSemesterIds(): List<String>
    suspend fun fetchExaminations(semester: String, nameOrNumber: String = ""): List<Examination>
    suspend fun fetchGradeReport(semester: String): TermGradeReport
}
