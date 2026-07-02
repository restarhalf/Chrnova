package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 课程数据模型
 *
 * 表示一门课程的完整信息，包括时间、地点、教师等。
 * 支持序列化用于本地存储和网络传输。
 */
@Immutable
@Serializable
data class Course(
    /** 课程本地数据库ID */
    val id: Long = 0,
    /** 课程名称 */
    val name: String,
    /** 学期ID，用于区分不同学期的课程 */
    val semesterId: String = "",
    /** 上课地点 */
    val location: String,
    /** 授课教师 */
    val teacher: String,
    /** 星期几上课（1=周一，7=周日） */
    val dayOfWeek: Int,
    /** 开始节次（从1开始） */
    val startSection: Int,
    /** 持续节数 */
    val sectionCount: Int,
    /** 上课周次列表，如[1,3,5,7,9]表示单周上课 */
    val weeks: List<Int>,
    /** 课程颜色（十六进制字符串，如"#FF5722"） */
    val color: String,
    /** 课程类型：0=普通课程，1=实验课，2=调课 */
    val type: Int = 0,
    /** 远程教务系统中的课程唯一标识 */
    val remoteKey: String = "",
    /** 原始远程标识（用于调课场景，记录调课前的原始课程） */
    val originRemoteKey: String? = null,
    /** 调课目标周次（仅调课类型课程有效） */
    val targetWeek: Int = 0,
    /** 关联的学号（用户账号） */
    val userNo: String = "",
)
