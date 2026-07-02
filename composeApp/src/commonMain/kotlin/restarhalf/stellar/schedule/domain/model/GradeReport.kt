package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * 课程成绩数据模型
 *
 * 表示一门课程的详细成绩信息，包括分数、绩点、学分等。
 */
@Immutable
@Serializable
data class GradeCourse(
    /** 课程代码 */
    val courseCode: String = "",
    /** 课程名称 */
    val courseName: String = "",
    /** 成绩分数（如"85"、"优秀"等） */
    val score: String = "",
    /** 绩点（如3.5） */
    val gradePoint: Double = 0.0,
    /** 学分 */
    val credit: Double = 0.0,
    /** 课程属性（如"必修"、"选修"） */
    val curriculumAttributes: String = "",
    /** 课程性质（如"公共基础课"、"专业课"） */
    val courseNature: String = "",
    /** 考试名称（如"期末考试"） */
    val examName: String = "",
    /** 考试性质（如"正常考试"、"补考"） */
    val examinationNature: String = "",
    /** 通过状态（如"通过"、"未通过"） */
    val passStatus: String = "",
    /** 成绩等级（如"A"、"B+"） */
    val gradeLevel: String = "",
    /** 成绩标记（如"正常"、"缓考"） */
    val markFlag: String = "",
    /** 重修学期（如"2023-2024-1"） */
    val repeatSemester: String = "",
    /** 成绩ID，用于唯一标识该成绩记录 */
    val gradeId: String = "",
    /** 学期（如"2023-2024-1"表示2023-2024学年第一学期） */
    val semester: String = "",
)

/**
 * 学期成绩报告数据模型
 *
 * 包含学生基本信息和该学期所有课程的成绩列表。
 */
@Immutable
@Serializable
data class TermGradeReport(
    /** 学号 */
    val studentId: String = "",
    /** 学生姓名 */
    val studentName: String = "",
    /** 入学年份 */
    val enrollmentYear: String = "",
    /** 平均分 */
    val averageScore: String = "",
    /** 已获得学分 */
    val earnedCredits: String = "",
    /** 总绩点 */
    val totalGradePoints: String = "",
    /** 平均学分绩点 */
    val averageCreditGradePoint: String = "",
    /** 该学期所有课程成绩列表 */
    val achievements: List<GradeCourse> = emptyList(),
)