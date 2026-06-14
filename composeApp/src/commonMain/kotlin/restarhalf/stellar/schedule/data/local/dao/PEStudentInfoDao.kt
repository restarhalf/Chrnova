package restarhalf.stellar.schedule.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.local.entity.PEStudentInfoEntity

@Dao
interface PEStudentInfoDao {
    @Query("SELECT * FROM pe_student_info WHERE id = 'current' LIMIT 1")
    fun observeStudentInfo(): Flow<PEStudentInfoEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentInfo(info: PEStudentInfoEntity)

    @Query("DELETE FROM pe_student_info")
    suspend fun deleteAll()
}