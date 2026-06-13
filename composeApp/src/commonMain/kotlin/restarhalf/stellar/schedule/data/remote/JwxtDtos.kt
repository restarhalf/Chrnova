package restarhalf.stellar.schedule.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * 教务系统API数据传输对象（DTO）
 * 
 * 定义与教务系统通信的所有数据结构，用于JSON序列化/反序列化。
 */

/** 登录响应 */
@Serializable
data class JwxtLoginResponse(
    @SerialName("code") val code: Int = -1,
    @SerialName("Msg") val msg: String = "",
    @SerialName("msg") val msgAlt: String = "",
    @SerialName("data") val data: JwxtLoginData? = null
) {
    fun messageOrEmpty(): String = msg.ifBlank { msgAlt }
}

/** 登录数据 */
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

/** 通用API响应 */
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

/** 当前学期项 */
@Serializable
data class JwxtCurrentTermItem(
    @SerialName("semesterId") val semesterId: String = "",
    @SerialName("semesterName") val semesterName: String = ""
)

/** 当前学期响应类型别名 */
typealias JwxtCurrentTermResponse = JwxtApiResponse<List<JwxtCurrentTermItem>>

/** 学期项 */
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

/** 教学周项 */
@Serializable
data class JwxtTeachingWeekItem(@SerialName("week") val week: String = "")

/** 教学周响应 */
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

/** 校区项 */
@Serializable
data class JwxtCampusItem(
    @SerialName("mrms") val isDefault: String = "",
    @SerialName("kbjcmsid") val kbjcmsid: String = "",
    @SerialName("kbjcmsmc") val kbjcmsmc: String = ""
) {
    fun isDefaultCampus(): Boolean = isDefault == "1"
}

/** 校区响应 */
@Serializable
data class JwxtCampusResponse(
    @SerialName("msg") val msg: String = "",
    @SerialName("code") val code: Int = -1,
    @SerialName("data") val data: List<JwxtCampusItem> = emptyList()
) {
    fun isSuccess(): Boolean = code == 1
}

/** 课程表响应 */
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

/** 周次字符串序列化器 */
private object WeekAsStringSerializer : KSerializer<String> {
    override val descriptor = buildClassSerialDescriptor("WeekAsString")

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)

    override fun deserialize(decoder: Decoder): String {
        val element = (decoder as kotlinx.serialization.json.JsonDecoder).decodeJsonElement()
        return element.jsonPrimitive.content
    }
}

/** 课程表日期块 */
@Serializable
data class JwxtCurriculumDayBlock(
    @SerialName("date") val date: List<JwxtDateItem> = emptyList(),
    @SerialName("item") val item: List<JwxtCurriculumItem> = emptyList(),
    @Serializable(with = WeekAsStringSerializer::class) @SerialName("week") val week: String = "",
    @SerialName("weekday") val weekday: String = ""
)

/** 日期项 */
@Serializable
data class JwxtDateItem(
    @SerialName("xqmc") val xqmc: String = "",
    @SerialName("mxrq") val mxrq: String = "",
    @SerialName("zc") val zc: String = "",
    @SerialName("xqid") val xqid: Int = 0
)

/** 课程项 */
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

/** 节次数据项 */
@Serializable
data class JwxtJcDataItem(
    @SerialName("XJMC") val xjmc: String = "",
    @SerialName("DJMC") val djmc: String = ""
)

/** 非课表项 */
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


/** 考试安排项 */
@Serializable
data class JwxtExaminationItem(
    @SerialName("courseName") val courseName: String = "",
    @SerialName("ksbz") val ksbz: String = "",
    @SerialName("courseNumber") val courseNumber: String = "",
    @SerialName("examinationPlace") val examinationPlace: String = "",
    @SerialName("zwh") val zwh: String = "",
    @SerialName("time") val time: String = ""
)

/** 考试安排响应 */
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


/** 成绩详情项 */
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

/** 学期成绩数据项 */
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

/** 学期成绩响应类型别名 */
typealias JwxtTermGradeResponse = JwxtApiResponse<List<JwxtTermGradeDataItem>>