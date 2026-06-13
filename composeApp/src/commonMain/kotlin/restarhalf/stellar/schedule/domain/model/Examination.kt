package restarhalf.stellar.schedule.domain.model

import kotlinx.serialization.Serializable

/**
 * 考试安排数据模型
 * 
 * 表示一门课程的考试信息，包括时间、地点、座位号等。
 */
@Serializable
data class Examination(
    /** 课程编号 */
    val courseNumber: String = "",
    /** 课程名称 */
    val courseName: String = "",
    /** 考试时间（如"2024-01-15 14:00-16:00"） */
    val time: String = "",
    /** 考试地点 */
    val examinationPlace: String = "",
    /** 座位号 */
    val zwh: String = "",
    /** 考试标志（如"正常"、"缓考"等） */
    val ksbz: String = "",
)
