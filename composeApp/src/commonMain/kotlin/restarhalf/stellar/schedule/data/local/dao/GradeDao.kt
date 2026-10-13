package restarhalf.stellar.schedule.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.local.entity.GradeEntity


@Dao
interface GradeDao {
    
    @Query("SELECT * FROM grades")
    fun observeAllGrades(): Flow<List<GradeEntity>>

    
    @Query("SELECT * FROM grades WHERE userNo = :userNo")
    fun observeGradesByUserNo(userNo: String): Flow<List<GradeEntity>>

    
    @Query("SELECT * FROM grades WHERE userNo = :userNo AND semester = :semester")
    fun observeGradesByUserNoAndSemester(userNo: String, semester: String): Flow<List<GradeEntity>>

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    
    @Query("DELETE FROM grades WHERE semester = :semester")
    suspend fun deleteGradesBySemester(semester: String)

    
    @Query("DELETE FROM grades WHERE userNo = :userNo AND semester = :semester")
    suspend fun deleteGradesByUserNoAndSemester(userNo: String, semester: String)

    
    @Query("SELECT * FROM grades WHERE userNo = :userNo")
    suspend fun getAllGradesByUserNo(userNo: String): List<GradeEntity>

    
    @Query("DELETE FROM grades")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceBySemester(semester: String, grades: List<GradeEntity>) {
        deleteGradesBySemester(semester)
        if (grades.isNotEmpty()) {
            insertGrades(grades)
        }
    }

    @Transaction
    suspend fun replaceByUserNoAndSemester(
        userNo: String,
        semester: String,
        grades: List<GradeEntity>
    ) {
        deleteGradesByUserNoAndSemester(userNo, semester)
        if (grades.isNotEmpty()) {
            insertGrades(grades)
        }
    }
}
