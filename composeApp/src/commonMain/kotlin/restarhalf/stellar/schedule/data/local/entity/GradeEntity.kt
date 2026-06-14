package restarhalf.stellar.schedule.data.local.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * 成绩Room实体
 * 
 * 映射到grades表，存储成绩信息。
 */
@Entity(tableName = "grades")
data class GradeEntity(
    /** ID（自动生成） */
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 课程代码 */
    val courseCode: String = "",
    /** 课程名称 */
    val courseName: String = "",
    /** 成绩 */
    val score: String = "",
    /** 绩点 */
    val gradePoint: Double = 0.0,
    /** 学分 */
    val credit: Double = 0.0,
    /** 课程属性 */
    val curriculumAttributes: String = "",
    /** 课程性质 */
    val courseNature: String = "",
    /** 考试名称 */
    val examName: String = "",
    /** 考试性质 */
    val examinationNature: String = "",
    /** 通过状态 */
    val passStatus: String = "",
    /** 成绩等级 */
    val gradeLevel: String = "",
    /** 成绩标识 */
    val markFlag: String = "",
    /** 补重修学期 */
    val repeatSemester: String = "",
    /** 成绩ID */
    val gradeId: String = "",
    /** 学期 */
    val semester: String = ""
)
