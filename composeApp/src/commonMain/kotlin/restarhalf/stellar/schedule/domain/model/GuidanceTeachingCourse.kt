package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 指导教学课程数据模型
 *
 * 表示创新创业专业融合选修或专业选修课程的信息。
 */
@Immutable
@Serializable
data class GuidanceTeachingCourse(
    /** 课程属性（如"创新创业专业融合教育选修"、"专业选修"） */
    val courseAttribute: String = "",
    /** 开课学期（如"1"、"2"） */
    val openSemester: String = "",
    /** 课程名称 */
    val courseName: String = "",
    /** 总学时 */
    val totalHours: String = "",
    /** 课程代码 */
    val courseCode: String = "",
    /** 课程类别名称（如"任选"） */
    val kclbmc: String = "",
    /** 开课单位 */
    val courseUnits: String = "",
    /** 是否有考试 */
    val whetherTest: String = "",
    /** 学分 */
    val credit: String = "",
    /** 考核方式 */
    val evaluationMode: String = "",
)
