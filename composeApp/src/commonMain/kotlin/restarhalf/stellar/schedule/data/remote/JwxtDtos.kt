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

/** 学期项（getXnxqList接口） */
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

/** 学期列表项（semesterList接口） */
@Serializable
data class JwxtSemesterListItem(
    @SerialName("semesterId") val semesterId: String = "",
    @SerialName("semesterName") val semesterName: String = "",
    @SerialName("nowXq") val nowXq: String = ""
) {
    fun isCurrent(): Boolean = nowXq == "1"
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

/** 指导教学课程项（创新创业专业融合选修/专业选修） */
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

/** 学时分类 */
@Serializable
data class ClassHourClassification(
    @SerialName("xsflid") val xsflid: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("classHour") val classHour: String = ""
)

/** 指导教学课程响应 */
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

// ==================== 选课系统 DTO ====================
// 对应 http://jwyd.dlnu.edu.cn/jsxsd/qzapp/* 系列接口

/**
 * 选课通用响应：errorCode 取值 success / success_needcf / fail。
 *
 * data 类型因接口而异（列表 / 对象 / 空字符串），统一以 [JsonElement] 兜底，
 * 由调用方按场景解析为具体的 [JwxtSelectionCourse] 列表或 [JwxtSelectionOperResult]。
 */
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

    /** 选课操作还需要选择关联教学班时返回 */
    fun isNeedConfirm(): Boolean = errorCode == "success_needcf"

    fun isFail(): Boolean = errorCode == "fail"

    /** 拼接错误信息，含模板参数替换（如 "还有[0]需要选" -> "还有[讲课学时]需要选"） */
    fun resolvedMessage(): String {
        var msg = errorMessage
        errorMessageParam.forEachIndexed { index, param ->
            msg = msg.replace("[$index]", param)
        }
        return msg
    }
}

/** 选课轮次项（wxgetXklc 接口的 data 数组元素） */
@Serializable
data class JwxtSelectionRotation(
    @SerialName("rotationid") val rotationId: String = "",
    @SerialName("rotationname") val rotationName: String = "",
    @SerialName("semesterid") val semesterId: String = "",
    @SerialName("xqmc") val xqmc: String = "",
    /** 选课开始时间，格式 yyyy-MM-dd HH:mm */
    @SerialName("xkkssj") val startTime: String = "",
    /** 选课结束时间，格式 yyyy-MM-dd HH:mm */
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

/** wxinitXscache 返回的选课规则数据（仅提取关键字段，其余忽略） */
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
    /** 注意：服务端返回字符串 "true" / "false" */
    @SerialName("courseQualification") val courseQualification: String = "",
)

/** 选课分类项（必修/选修/本学期计划等） */
@Serializable
data class JwxtSelectionClassification(
    @SerialName("classificationCode") val classificationCode: String = "",
    @SerialName("classificationName") val classificationName: String = "",
)

/** 课程列表项（wxgetKcList 返回元素） */
@Serializable
data class JwxtSelectionCourse(
    @SerialName("courseName") val courseName: String = "",
    @SerialName("courseNumber") val courseNumber: String = "",
    @SerialName("courseId") val courseId: String = "",
    @SerialName("noticeId") val noticeId: String = "",
    /** 教学班序号 */
    @SerialName("kxh") val kxh: String = "",
    @SerialName("classTeacher") val classTeacher: String = "",
    @SerialName("classPlace") val classPlace: String = "",
    @SerialName("classTime") val classTime: String = "",
    @SerialName("period") val period: String = "",
    @SerialName("credit") val credit: String = "",
    @SerialName("groupName") val groupName: String = "",
    @SerialName("splitIdentification") val splitIdentification: String = "",
) {
    /** 把 &nbsp; / <br> 转换为可读文本 */
    fun cleanPlace(): String = classPlace
        .replace("&nbsp;", " ")
        .replace("<br>", " / ")
        .trim()

    fun cleanTime(): String = classTime
        .replace("&nbsp;", " ")
        .replace("<br>", " / ")
        .trim()
}

/** 已选课程项（wxgetYxkcList 返回元素，含 isCanTk 退课标志） */
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
    /** "1"=可退课 */
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

/**
 * 选课操作解析后的统一结果。
 * - [Success]：选课成功，data 可能为关联课程列表
 * - [NeedConfirm]：success_needcf，需要继续选关联教学班
 * - [Fail]：选课失败（如已选其它教学班、容量已满等）
 * - [Unknown]：未知响应
 */
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