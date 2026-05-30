package restarhalf.stellar.schedule.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExaminationDao {
    @Query("SELECT * FROM examinations WHERE semesterId = :semesterId")
    fun observeExaminations(semesterId: String): Flow<List<ExaminationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExaminations(examinations: List<ExaminationEntity>)

    @Query("DELETE FROM examinations WHERE semesterId = :semesterId")
    suspend fun deleteExaminationsBySemester(semesterId: String)

    @Query("DELETE FROM examinations")
    suspend fun deleteAll()
}
