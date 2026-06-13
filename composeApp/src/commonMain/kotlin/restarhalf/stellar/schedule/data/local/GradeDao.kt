package restarhalf.stellar.schedule.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * 成绩数据访问对象（DAO）
 * 
 * 定义成绩表的所有数据库操作。
 */
@Dao
interface GradeDao {
    /**
     * 观察所有成绩
     * 
     * @return 成绩列表Flow
     */
    @Query("SELECT * FROM grades")
    fun observeAllGrades(): Flow<List<GradeEntity>>

    /**
     * 批量插入成绩
     * 
     * @param grades 成绩列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    /**
     * 按学期删除成绩
     * 
     * @param semester 学期
     */
    @Query("DELETE FROM grades WHERE semester = :semester")
    suspend fun deleteGradesBySemester(semester: String)

    /** 删除所有成绩 */
    @Query("DELETE FROM grades")
    suspend fun deleteAll()
}
