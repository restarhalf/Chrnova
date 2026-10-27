package restarhalf.stellar.schedule.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.local.entity.ExaminationEntity

@Dao
interface ExaminationDao {
    
    @Query("SELECT * FROM examinations")
    fun observeAllExaminations(): Flow<List<ExaminationEntity>>

    @Query("SELECT * FROM examinations")
    suspend fun getAllExaminationsOnce(): List<ExaminationEntity>

    @Query("SELECT * FROM examinations WHERE userNo = :userNo")
    fun observeExaminationsByUserNo(userNo: String): Flow<List<ExaminationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExaminations(examinations: List<ExaminationEntity>)

    @Query("DELETE FROM examinations WHERE semesterId = :semesterId")
    suspend fun deleteExaminationsBySemester(semesterId: String)

    @Query("SELECT * FROM examinations WHERE id = :id")
    fun observeExaminationById(id: Long): Flow<ExaminationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamination(examination: ExaminationEntity): Long

    @Query("DELETE FROM examinations WHERE id = :id")
    suspend fun deleteExamination(id: Long)

    @Query("DELETE FROM examinations")
    suspend fun deleteAll()

    @Query("UPDATE examinations SET userNo = :userNo WHERE userNo = ''")
    suspend fun bindUnboundExaminations(userNo: String)

    @Query("UPDATE examinations SET semesterId = :newSemesterId WHERE semesterId = :oldSemesterId")
    suspend fun updateSemesterId(oldSemesterId: String, newSemesterId: String)

    @Query("SELECT COUNT(*) FROM examinations WHERE semesterId = '' OR semesterId = 'manual'")
    suspend fun countExamsWithInvalidSemester(): Int

    @Query("DELETE FROM examinations WHERE semesterId = :semesterId AND source = 'sync'")
    suspend fun deleteSyncExaminationsBySemester(semesterId: String)

    @Transaction
    suspend fun replaceBySemester(semesterId: String, examinations: List<ExaminationEntity>) {
        deleteSyncExaminationsBySemester(semesterId)
        if (examinations.isNotEmpty()) {
            insertExaminations(examinations)
        }
    }
}
