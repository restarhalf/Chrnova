package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.GradeCourse

/**
 * 成绩仓库接口
 * 
 * 定义成绩数据的访问抽象接口。
 */
interface GradeRepository {
    /**
     * 观察所有成绩
     *
     * @return 成绩列表Flow
     */
    fun observeAllGrades(): Flow<List<GradeCourse>>

    /**
     * 按学号观察成绩
     *
     * @param userNo 学号
     * @return 成绩列表Flow
     */
    fun observeGradesByUserNo(userNo: String): Flow<List<GradeCourse>>

    /**
     * 按学号查询所有成绩（一次性读取，用于离线回退）
     *
     * @param userNo 学号
     * @return 成绩列表
     */
    suspend fun getAllGradesByUserNo(userNo: String): List<GradeCourse>

    /**
     * 替换成绩数据
     *
     * @param semester 学期
     * @param grades 成绩列表
     */
    suspend fun replaceGrades(semester: String, grades: List<GradeCourse>)

    /**
     * 按学号和学期替换成绩数据
     *
     * @param userNo 学号
     * @param semester 学期
     * @param grades 成绩列表
     */
    suspend fun replaceGradesByUserNoAndSemester(
        userNo: String,
        semester: String,
        grades: List<GradeCourse>
    )

    /**
     * 清除所有成绩
     */
    suspend fun clearAll()
}
