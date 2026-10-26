package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class JwxtSelectionRotation(
    @SerialName("rotationid") val rotationId: String = "",
    @SerialName("rotationname") val rotationName: String = "",
    @SerialName("semesterid") val semesterId: String = "",
    @SerialName("xqmc") val xqmc: String = "",

    @SerialName("xkkssj") val startTime: String = "",

    @SerialName("xkjzsj") val endTime: String = "",
    @SerialName("courseselectiontime") val courseselectiontime: String = "",
    @SerialName("courseselectioncontrol") val courseselectioncontrol: String = "",
    @SerialName("dailycourseselection") val dailycourseselection: String = "",
    @SerialName("preselectedcourses") val preselectedcourses: String = "",
    @SerialName("drawlots") val drawlots: String = "",
    @SerialName("populationcontrol") val populationcontrol: String = "",
    @SerialName("conflictselection") val conflictselection: String = "",
    @SerialName("creditcontrol") val creditcontrol: String? = null,
    @SerialName("xkbs") val xkbs: String = "",
)

@Immutable
@Serializable
data class JwxtSelectionInitData(
    @SerialName("sessionTime") val sessionTime: String = "",
    @SerialName("classificationList") val classificationList: List<JwxtSelectionClassification> = emptyList(),
    @SerialName("gzsmStr") val gzsmStr: String = "",
    @SerialName("compulsorySelection") val compulsorySelection: Boolean = false,
    @SerialName("compulsorySemester") val compulsorySemester: Boolean = false,
    @SerialName("compulsoryGrades") val compulsoryGrades: Boolean = false,
    @SerialName("selectionGrades") val selectionGrades: Boolean = false,
    @SerialName("departmentCurriculum") val departmentCurriculum: Boolean = false,

    @SerialName("courseQualification") val courseQualification: String = "",
)

@Immutable
@Serializable
data class JwxtSelectionClassification(
    @SerialName("classificationCode") val classificationCode: String = "",
    @SerialName("classificationName") val classificationName: String = "",
)

@Immutable
@Serializable
data class JwxtSelectionCourse(
    @SerialName("courseName") val courseName: String = "",
    @SerialName("courseNumber") val courseNumber: String = "",
    @SerialName("courseId") val courseId: String = "",
    @SerialName("noticeId") val noticeId: String = "",

    @SerialName("kxh") val kxh: String = "",
    @SerialName("classTeacher") val classTeacher: String = "",
    @SerialName("classPlace") val classPlace: String = "",
    @SerialName("classTime") val classTime: String = "",
    @SerialName("period") val period: String = "",
    @SerialName("credit") val credit: String = "",
    @SerialName("groupName") val groupName: String = "",
    @SerialName("splitIdentification") val splitIdentification: String = "",
) {

    fun cleanPlace(): String = classPlace
        .replace("&nbsp;", " ")
        .replace("<br>", " / ")
        .trim()

    fun cleanTime(): String = classTime
        .replace("&nbsp;", " ")
        .replace("<br>", " / ")
        .trim()
}

@Immutable
@Serializable
data class JwxtSelectedCourse(
    @SerialName("courseName") val courseName: String = "",
    @SerialName("courseNumber") val courseNumber: String = "",
    @SerialName("noticeId") val noticeId: String = "",
    @SerialName("kxh") val kxh: String = "",
    @SerialName("classTeacher") val classTeacher: String = "",
    @SerialName("classPlace") val classPlace: String = "",
    @SerialName("classTime") val classTime: String = "",
    @SerialName("period") val period: String = "",
    @SerialName("credit") val credit: String = "",
    @SerialName("groupName") val groupName: String = "",

    @SerialName("isCanTk") val isCanTk: String = "0",
    @SerialName("kclb") val kclb: String = "",
) {
    fun cleanPlace(): String = classPlace
        .replace("&nbsp;", " ")
        .replace("<br>", " / ")
        .trim()

    fun cleanTime(): String = classTime
        .replace("&nbsp;", " ")
        .replace("<br>", " / ")
        .trim()

    val canDrop: Boolean get() = isCanTk == "1"
}

sealed class JwxtSelectionOperResult {
    data class Success(val message: String, val relatedCourses: List<JwxtSelectionCourse> = emptyList()) :
        JwxtSelectionOperResult()

    data class NeedConfirm(
        val message: String,
        val yxcfbs: String,
        val cfbs: String,
        val xkkcid: String,
        val yxjx0404id: String,
    ) : JwxtSelectionOperResult()

    data class Fail(val message: String) : JwxtSelectionOperResult()

    data class Unknown(val errorCode: String, val message: String) : JwxtSelectionOperResult()
}
