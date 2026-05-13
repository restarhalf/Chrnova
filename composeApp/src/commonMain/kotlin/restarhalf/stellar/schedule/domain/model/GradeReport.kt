package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GradeCourse(
    val courseCode: String = "",
    val courseName: String = "",
    val score: String = "",
    val gradePoint: Double = 0.0,
    val credit: Double = 0.0,
    val curriculumAttributes: String = "",
    val courseNature: String = "",
    val examName: String = "",
    val examinationNature: String = "",
    val passStatus: String = "",
    val gradeLevel: String = "",
    val markFlag: String = "",
    val repeatSemester: String = "",
    val gradeId: String = "",
    val semester: String = "",
)

@Serializable
data class TermGradeReport(
    val studentId: String = "",
    val studentName: String = "",
    val enrollmentYear: String = "",
    val averageScore: String = "",
    val earnedCredits: String = "",
    val totalGradePoints: String = "",
    val averageCreditGradePoint: String = "",
    val achievements: List<GradeCourse> = emptyList(),
)