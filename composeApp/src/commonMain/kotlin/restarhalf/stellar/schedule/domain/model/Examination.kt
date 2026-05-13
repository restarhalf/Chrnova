package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Examination(
    val courseNumber: String = "",
    val courseName: String = "",
    val time: String = "",
    val examinationPlace: String = "",
    val zwh: String = "",
    val ksbz: String = "",
)
