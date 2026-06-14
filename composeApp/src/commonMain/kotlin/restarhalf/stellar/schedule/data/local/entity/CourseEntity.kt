package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * 课程Room实体
 * 
 * 映射到courses表，存储课程信息。
 */
@Entity(tableName = "courses")
data class CourseEntity(
    /** 课程ID（自动生成） */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 课程名称 */
    val name: String,
    /** 学期ID */
    val semesterId: String = "",
    /** 上课地点 */
    val location: String,
    /** 教师 */
    val teacher: String,
    /** 星期几（1-7） */
    val dayOfWeek: Int,
    /** 开始节次 */
    val startSection: Int,
    /** 持续节数 */
    val sectionCount: Int,
    /** 上课周次列表 */
    val weeks: List<Int>,
    /** 课程颜色 */
    val color: String,
    /** 课程类型（0=普通，1=实验，2=调课） */
    val type: Int = 0,
    /** 远程标识 */
    val remoteKey: String = "",
    /** 原始远程标识（调课时使用） */
    val originRemoteKey: String? = null,
    /** 目标周次（调课时使用） */
    val targetWeek: Int = 0,
)
