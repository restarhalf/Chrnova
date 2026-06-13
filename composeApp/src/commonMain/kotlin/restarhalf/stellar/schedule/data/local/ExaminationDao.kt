package restarhalf.stellar.schedule.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

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

    /** 删除所有考试安排 */
    @Query("DELETE FROM examinations")
    suspend fun deleteAll()
}
