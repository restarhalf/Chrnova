package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.GuidanceTeachingCourse
import restarhalf.stellar.schedule.domain.model.RemoteCampus
import restarhalf.stellar.schedule.domain.model.TermGradeReport

/**
 * 教务数据端口接口
 *
 * 定义从教务系统获取学术数据的抽象接口，包括学期信息、考试安排、成绩报告等。
 */
interface AcademicPort {
    /** 获取当前学期ID */
    suspend fun fetchCurrentTermId(): String
    /** 获取所有校区列表 */
    suspend fun fetchCampuses(): List<RemoteCampus>
    /** 获取所有可用学期ID列表 */
    suspend fun fetchSemesterIds(): List<String>
    /**
     * 获取考试安排列表
     *
     * @param semester 学期ID
     * @param nameOrNumber 课程名称或编号筛选（可选）
     * @return 考试安排列表
     */
    suspend fun fetchExaminations(semester: String, nameOrNumber: String = ""): List<Examination>
    /**
     * 获取学期成绩报告
     *
     * @param semester 学期ID
     * @return 学期成绩报告，包含所有课程成绩
     */
    suspend fun fetchGradeReport(semester: String): TermGradeReport

    /**
     * 获取指导教学课程列表（创新创业专业融合选修/专业选修）
     *
     * @param kcxz 课程性质（54=创新创业专业融合教育选修，61=专业选修）
     * @param kcsx 课程属性筛选（可选）
     * @param kcmc 课程名称筛选（可选）
     * @return 指导教学课程列表
     */
    suspend fun fetchGuidanceTeachingCourses(
        kcxz: String,
        kcsx: String = "",
        kcmc: String = ""
    ): List<GuidanceTeachingCourse>

    /**
     * 获取教学周总周数
     *
     * @return 总周数，获取失败时返回 0
     */
    suspend fun fetchTeachingWeekTotal(): Int
}
