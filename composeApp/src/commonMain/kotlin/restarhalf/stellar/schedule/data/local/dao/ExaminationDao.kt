package restarhalf.stellar.schedule.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.local.entity.ExaminationEntity

/**
 * 考试安排数据访问对象（DAO）
 * 
 * 定义考试安排表的所有数据库操作。
 */
@Dao
interface ExaminationDao {
    /**
     * 观察所有考试安排
     * 
     * @return 考试安排列表Flow
     */
    @Query("SELECT * FROM examinations")
    fun observeAllExaminations(): Flow<List<ExaminationEntity>>

    /**
     * 按学号观察考试安排
     * 
     * @param userNo 学号
     * @return 考试安排列表Flow
     */
    @Query("SELECT * FROM examinations WHERE userNo = :userNo")
    fun observeExaminationsByUserNo(userNo: String): Flow<List<ExaminationEntity>>

    /**
     * 批量插入考试安排
     * 
     * @param examinations 考试安排列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExaminations(examinations: List<ExaminationEntity>)

    /**
     * 按学期删除考试安排
     * 
     * @param semesterId 学期ID
     */
    @Query("DELETE FROM examinations WHERE semesterId = :semesterId")
    suspend fun deleteExaminationsBySemester(semesterId: String)

    /**
     * 按ID观察单个考试安排
     * 
     * @param id 考试ID
     * @return 考试安排Flow
     */
    @Query("SELECT * FROM examinations WHERE id = :id")
    fun observeExaminationById(id: Long): Flow<ExaminationEntity?>

    /**
     * 插入单个考试安排
     * 
     * @param examination 考试安排
     * @return 插入的行ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamination(examination: ExaminationEntity): Long

    /**
     * 删除单个考试安排
     * 
     * @param examination 考试安排
     */
    @androidx.room3.Query("DELETE FROM examinations WHERE id = :id")
    suspend fun deleteExamination(id: Long)

    /** 删除所有考试安排 */
    @Query("DELETE FROM examinations")
    suspend fun deleteAll()

    /** 将未绑定学号的考试绑定到指定学号 */
    @Query("UPDATE examinations SET userNo = :userNo WHERE userNo = ''")
    suspend fun bindUnboundExaminations(userNo: String)
}
