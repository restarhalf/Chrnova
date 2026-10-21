package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.JwxtSelectedCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionClassification
import restarhalf.stellar.schedule.domain.model.JwxtSelectionCourse
import restarhalf.stellar.schedule.domain.model.JwxtSelectionOperResult
import restarhalf.stellar.schedule.domain.model.JwxtSelectionRotation


interface CourseSelectionPort {

    suspend fun fetchRotations(): List<JwxtSelectionRotation>


    suspend fun initSession(rotationId: String): JwxtSelectionSession


    suspend fun fetchCourses(
        rotationId: String,
        classificationCode: String,
        sessionTime: String,
        extraRules: Map<String, String>,
        courseInformation: String = "",
    ): List<JwxtSelectionCourse>


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
    ): JwxtSelectionDropResult


    suspend fun fetchSelectedCourses(rotationId: String): List<JwxtSelectedCourse>
}


data class JwxtSelectionSession(
    val sessionTime: String,
    val classifications: List<JwxtSelectionClassification>,
    val extraRules: Map<String, String>,
)


data class JwxtSelectionDropResult(
    val success: Boolean,
    val message: String,
)
