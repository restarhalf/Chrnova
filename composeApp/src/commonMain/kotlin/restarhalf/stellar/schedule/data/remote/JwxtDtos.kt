package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive




@Serializable
data class JwxtLoginResponse(
    @SerialName("code") val code: Int = -1,
    @SerialName("Msg") val msg: String = "",
    @SerialName("msg") val msgAlt: String = "",
    @SerialName("data") val data: JwxtLoginData? = null
) {
    fun messageOrEmpty(): String = msg.ifBlank { msgAlt }
}


@Serializable
data class JwxtLoginData(
    @SerialName("birthday") val birthday: String = "",
    @SerialName("academyName") val academyName: String = "",
    @SerialName("userNo") val userNo: String = "",
    @SerialName("entranceYear") val entranceYear: String = "",
    @SerialName("clsName") val clsName: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("token") val token: String = "",
    @SerialName("userType") val userType: String = "",
    @SerialName("iscsh") val isFirstLogin: String? = null
)


@Serializable
data class JwxtApiResponse<T>(
    @SerialName("code") val code: String = "",
    @SerialName("Msg") val msg: String = "",
    @SerialName("msg") val msgAlt: String = "",
    @SerialName("data") val data: T? = null
) {
    fun isSuccess(): Boolean = code == "1"

    fun messageOrEmpty(): String = msg.ifBlank { msgAlt }
}


@Serializable
data class JwxtCurrentTermItem(
    @SerialName("semesterId") val semesterId: String = "",
    @SerialName("semesterName") val semesterName: String = ""
)


typealias JwxtCurrentTermResponse = JwxtApiResponse<List<JwxtCurrentTermItem>>


@Serializable
data class JwxtSemesterItem(
    @SerialName("num") val num: String = "",
    @SerialName("isdqxq") val isCurrentTerm: String = "",
    @SerialName("xnxq01id") val xnxq01id: String = "",
    @SerialName("jsxn") val jsxn: String = "",
    @SerialName("ksxn") val ksxn: String = "",
    @SerialName("xq") val xq: String = "",
    @SerialName("curnum") val curnum: String = "",
    @SerialName("xqmc") val xqmc: String = ""
) {
    fun isCurrent(): Boolean = isCurrentTerm == "1"
}


@Serializable
data class JwxtSemesterListItem(
    @SerialName("semesterId") val semesterId: String = "",
    @SerialName("semesterName") val semesterName: String = "",
    @SerialName("nowXq") val nowXq: String = ""
) {
    fun isCurrent(): Boolean = nowXq == "1"
}


@Serializable
data class JwxtTeachingWeekItem(@SerialName("week") val week: String = "")


@Serializable
data class JwxtTeachingWeekResponse(
    @SerialName("isNowWeek") val isNowWeek: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("Msg") val msg: String = "",
    @SerialName("data") val data: List<JwxtTeachingWeekItem> = emptyList(),
    @SerialName("nowWeek") val nowWeek: String = ""
) {
    fun isSuccess(): Boolean = code == "1"
}


@Serializable
data class JwxtCampusItem(
    @SerialName("mrms") val isDefault: String = "",
    @SerialName("kbjcmsid") val kbjcmsid: String = "",
    @SerialName("kbjcmsmc") val kbjcmsmc: String = ""
) {
    fun isDefaultCampus(): Boolean = isDefault == "1"
}


@Serializable
data class JwxtCampusResponse(
    @SerialName("msg") val msg: String = "",
    @SerialName("code") val code: Int = -1,
    @SerialName("data") val data: List<JwxtCampusItem> = emptyList()
) {
    fun isSuccess(): Boolean = code == 1
}


@Serializable
data class JwxtCurriculumResponse(
    @SerialName("Msg") val msg: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("data") val data: List<JwxtCurriculumDayBlock> = emptyList(),
    @SerialName("nowWeek") val nowWeek: String = "",
    @SerialName("jcdatalist") val jcDataList: List<JwxtJcDataItem> = emptyList(),
    @SerialName("nkbList") val nkbList: List<JwxtNkbItem> = emptyList()
) {
    fun isSuccess(): Boolean = code == "1"
}


private object WeekAsStringSerializer : KSerializer<String> {
    override val descriptor = buildClassSerialDescriptor("WeekAsString")

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as kotlinx.serialization.json.JsonDecoder).decodeJsonElement()
        return element.jsonPrimitive.content
    }
}


@Serializable
data class JwxtCurriculumDayBlock(
    @SerialName("date") val date: List<JwxtDateItem> = emptyList(),
    @SerialName("item") val item: List<JwxtCurriculumItem> = emptyList(),
    @Serializable(with = WeekAsStringSerializer::class) @SerialName("week") val week: String = "",
    @SerialName("weekday") val weekday: String = ""
)


@Serializable
data class JwxtDateItem(
    @SerialName("xqmc") val xqmc: String = "",
    @SerialName("mxrq") val mxrq: String = "",
    @SerialName("zc") val zc: String = "",
    @SerialName("xqid") val xqid: Int = 0
)


@Serializable
data class JwxtCurriculumItem(
    @SerialName("classWeek") val classWeek: String = "",
    @SerialName("teacherName") val teacherName: String = "",
    @SerialName("xqName") val xqName: String = "",
    @SerialName("weekNoteDetail") val weekNoteDetail: String = "",
    @SerialName("buttonCode") val buttonCode: String = "",
    @SerialName("xqNumber") val xqNumber: String = "",
    @SerialName("ktmc") val ktmc: String = "",
    @SerialName("classTime") val classTime: String = "",
    @SerialName("jx0408id") val jx0408id: String = "",
    @SerialName("kch") val kch: String = "",
    @SerialName("courseName") val courseName: String = "",
    @SerialName("isRepeatCode") val isRepeatCode: String = "",
    @SerialName("zxs") val zxs: Int = 0,
    @SerialName("peopleNumber") val peopleNumber: Int = 0,
    @SerialName("maxClassTime") val maxClassTime: String = "",
    @SerialName("khfs") val khfs: String = "",
    @SerialName("startTime") val startTime: String = "",
    @SerialName("endTIme") val endTime: String = "",
    @SerialName("location") val location: String = "",
    @SerialName("classWeekDetails") val classWeekDetails: String = "",
    @SerialName("coursesNote") val coursesNote: Int = 0
)


@Serializable
data class JwxtJcDataItem(
    @SerialName("XJMC") val xjmc: String = "",
    @SerialName("DJMC") val djmc: String = ""
)


@Serializable
data class JwxtNkbItem(
    @SerialName("kch") val kch: String = "",
    @SerialName("kcmc") val kcmc: String = "",
    @SerialName("jgxm") val jgxm: String = "",
    @SerialName("xqname") val xqname: String = "",
    @SerialName("zc") val zc: String = "",
    @SerialName("tzdlb") val tzdlb: String = "",
    @SerialName("sjzcbz") val sjzcbz: String = ""
)



@Serializable
data class JwxtExaminationItem(
    @SerialName("courseName") val courseName: String = "",
    @SerialName("ksbz") val ksbz: String = "",
    @SerialName("courseNumber") val courseNumber: String = "",
    @SerialName("examinationPlace") val examinationPlace: String = "",
    @SerialName("zwh") val zwh: String = "",
    @SerialName("time") val time: String = ""
)


@Serializable
data class JwxtExaminationResponse(
    @SerialName("Msg") val msg: String = "",
    @SerialName("msg") val msgAlt: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("data") val data: List<JwxtExaminationItem> = emptyList()
) {
    fun isSuccess(): Boolean = code == "1"

    fun messageOrEmpty(): String = msg.ifBlank { msgAlt }
}



@Serializable
data class JwxtGradeAchievementItem(
    @SerialName("curriculumAttributes") val curriculumAttributes: String = "",
    @SerialName("cjdj") val gradeLevel: String = "",
    @SerialName("sfjg") val passStatus: String = "",
    @SerialName("examName") val examName: String = "",
    @SerialName("courseNature") val courseNature: String = "",
    @SerialName("kkxq") val semester: String = "",
    @SerialName("kcbh") val courseCode: String = "",
    @SerialName("fraction") val score: String = "",
    @SerialName("courseName") val courseName: String = "",
    @SerialName("bcxq") val repeatSemester: String = "",
    @SerialName("cjbs") val markFlag: String = "",
    @SerialName("examinationNature") val examinationNature: String = "",
    @SerialName("jd") val gradePoint: Double = 0.0,
    @SerialName("credit") val credit: Double = 0.0,
    @SerialName("cj0708id") val gradeId: String = "",
)


@Serializable
data class JwxtTermGradeDataItem(
    @SerialName("studentID") val studentId: String = "",
    @SerialName("xqgpa") val semesterGpa: List<JsonElement> = emptyList(),
    @SerialName("inGrade") val enrollmentYear: String = "",
    @SerialName("pjcj") val averageScore: String = "",
    @SerialName("achievement") val achievement: List<JwxtGradeAchievementItem> = emptyList(),
    @SerialName("name") val name: String = "",
    @SerialName("yxzxf") val earnedCredits: String = "",
    @SerialName("zxfjd") val totalGradePoints: String = "",
    @SerialName("pjxfjd") val averageCreditGradePoint: String = "",
)


typealias JwxtTermGradeResponse = JwxtApiResponse<List<JwxtTermGradeDataItem>>


@Serializable
data class JwxtGuidanceTeachingItem(
    @SerialName("courseAttribute") val courseAttribute: String = "",
    @SerialName("openSemester") val openSemester: String = "",
    @SerialName("courseName") val courseName: String = "",
    @SerialName("totalHours") val totalHours: String = "",
    @SerialName("courseCode") val courseCode: String = "",
    @SerialName("kclbmc") val kclbmc: String = "",
    @SerialName("classHourClassification") val classHourClassification: List<ClassHourClassification> = emptyList(),
    @SerialName("courseUnits") val courseUnits: String = "",
    @SerialName("whetherTest") val whetherTest: String = "",
    @SerialName("credit") val credit: String = "",
    @SerialName("evaluationMode") val evaluationMode: String = ""
)


@Serializable
data class ClassHourClassification(
    @SerialName("xsflid") val xsflid: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("classHour") val classHour: String = ""
)


@Serializable
data class JwxtGuidanceTeachingResponse(
    @SerialName("Msg") val msg: String = "",
    @SerialName("msg") val msgAlt: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("data") val data: List<JwxtGuidanceTeachingItem> = emptyList()
) {
    fun isSuccess(): Boolean = code == "1"

    fun messageOrEmpty(): String = msg.ifBlank { msgAlt }
}





@Serializable
data class JwxtSelectionResponse(
    @SerialName("errorCode") val errorCode: String = "",
    @SerialName("errorMessage") val errorMessage: String = "",
    @SerialName("errorMessageParam") val errorMessageParam: List<String> = emptyList(),
    @SerialName("data") val data: JsonElement? = null,
    @SerialName("data_encryptStr") val dataEncryptStr: String = "",
    @SerialName("runTime") val runTime: String = "",
) {
    fun isSuccess(): Boolean = errorCode == "success"

    
    fun isNeedConfirm(): Boolean = errorCode == "success_needcf"

    fun isFail(): Boolean = errorCode == "fail"

    
    fun resolvedMessage(): String {
        var msg = errorMessage
        errorMessageParam.forEachIndexed { index, param ->
            msg = msg.replace("[$index]", param)
        }
        return msg
    }
}


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


@Serializable
data class JwxtSelectionClassification(
    @SerialName("classificationCode") val classificationCode: String = "",
    @SerialName("classificationName") val classificationName: String = "",
)


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