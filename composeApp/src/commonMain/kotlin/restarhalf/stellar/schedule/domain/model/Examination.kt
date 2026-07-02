package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 考试安排数据模型
 *
 * 表示一门课程的考试信息，包括时间、地点、座位号等。
 */
@Immutable
@Serializable
data class Examination(
    /** 数据库ID */
    val id: Long = 0,
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
    /** 数据来源："sync"=教务同步，"manual"=手动添加 */
    val source: String = "sync",
    /** 关联的学号（用户账号） */
    val userNo: String = "",
)
