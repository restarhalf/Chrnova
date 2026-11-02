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
    
    @Query("SELECT * FROM examinations WHERE userNo = :userNo")
    fun observeExaminationsByUserNo(userNo: String): Flow<List<ExaminationEntity>>

    @Query("SELECT * FROM examinations WHERE userNo = :userNo")
    suspend fun getAllExaminationsByUserNoOnce(userNo: String): List<ExaminationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExaminations(examinations: List<ExaminationEntity>)

    @Query("SELECT * FROM examinations WHERE id = :id")
    fun observeExaminationById(id: Long): Flow<ExaminationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamination(examination: ExaminationEntity): Long

    @Query("DELETE FROM examinations WHERE id = :id AND userNo = :userNo")
    suspend fun deleteExamination(id: Long, userNo: String)

    @Query(
        "UPDATE examinations SET semesterId = :newSemesterId " +
            "WHERE userNo = :userNo AND semesterId = :oldSemesterId"
    )
    suspend fun updateSemesterId(userNo: String, oldSemesterId: String, newSemesterId: String)

    @Query("DELETE FROM examinations WHERE userNo = :userNo AND semesterId = :semesterId AND source = 'sync'")
    suspend fun deleteSyncExaminationsByUserNoAndSemester(userNo: String, semesterId: String)

    @Transaction
    suspend fun replaceBySemester(
        userNo: String,
        semesterId: String,
        examinations: List<ExaminationEntity>,
    ) {
        deleteSyncExaminationsByUserNoAndSemester(userNo, semesterId)
        if (examinations.isNotEmpty()) {
            insertExaminations(examinations)
        }
    }
}
