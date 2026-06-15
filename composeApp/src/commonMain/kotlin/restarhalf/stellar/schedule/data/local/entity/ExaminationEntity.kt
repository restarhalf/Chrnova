package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * 考试安排Room实体
 * 
 * 映射到examinations表，存储考试安排信息。
 */
@Entity(tableName = "examinations")
data class ExaminationEntity(
    /** ID（自动生成） */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 课程编号 */
    val courseNumber: String = "",
    /** 课程名称 */
    val courseName: String = "",
    /** 考试时间 */
    val time: String = "",
    /** 考试地点 */
    val examinationPlace: String = "",
    /** 座位号 */
    val zwh: String = "",
    /** 考试标志 */
    val ksbz: String = "",
    /** 学期ID */
    val semesterId: String = "",
    /** 数据来源："sync"=教务同步，"manual"=手动添加 */
    val source: String = "sync"
)
