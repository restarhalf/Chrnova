package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination

/**
 * 考试安排仓库接口
 * 
 * 定义考试安排数据的访问抽象接口。
 */
interface ExaminationRepository {
    /**
     * 观察所有考试安排
     * 
     * @return 考试安排列表Flow
     */
    fun observeAllExaminations(): Flow<List<Examination>>

    /**
     * 按学号观察考试安排
     * 
     * @param userNo 学号
     * @return 考试安排列表Flow
     */
    fun observeExaminationsByUserNo(userNo: String): Flow<List<Examination>>

    /**
     * 按ID观察单个考试安排
     * 
     * @param id 考试ID
     * @return 考试安排Flow
     */
    fun observeExaminationById(id: Long): Flow<Examination?>

    /**
     * 保存单个考试安排（插入或更新）
     * 
     * @param examination 考试安排
     * @param semesterId 学期ID
     * @return 保存的行ID
     */
    suspend fun saveExamination(examination: Examination, semesterId: String): Long

    /**
     * 删除单个考试安排
     * 
     * @param id 考试ID
     */
    suspend fun deleteExamination(id: Long)

    /**
     * 替换考试安排数据
     * 
     * @param semesterId 学期ID
     * @param examinations 考试安排列表
     */
    suspend fun replaceExaminations(semesterId: String, examinations: List<Examination>)

    /**
     * 清除所有考试安排
     */
    suspend fun clearAll()

    /**
     * 将未绑定学号的考试绑定到指定学号
     *
     * @param userNo 学号
     */
    suspend fun bindUnboundExaminations(userNo: String)
}
