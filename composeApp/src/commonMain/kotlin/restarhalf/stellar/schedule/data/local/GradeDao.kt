package restarhalf.stellar.schedule.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades")
    fun observeAllGrades(): Flow<List<GradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Query("DELETE FROM grades WHERE semester = :semester")
    suspend fun deleteGradesBySemester(semester: String)

    @Query("DELETE FROM grades")
    suspend fun deleteAll()
}
