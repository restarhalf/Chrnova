package restarhalf.stellar.schedule.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.local.entity.PEDetailSummaryEntity
import restarhalf.stellar.schedule.data.local.entity.PESubjectScoreEntity

@Dao
interface PEDetailDao {
    @Query("SELECT * FROM pe_detail_scores WHERE userNo = :userNo AND schoolYear = :schoolYear")
    fun observeDetailScores(userNo: String, schoolYear: String): Flow<List<PESubjectScoreEntity>>

    @Query(
        "SELECT * FROM pe_detail_summary " +
            "WHERE userNo = :userNo AND schoolYear = :schoolYear LIMIT 1"
    )
    fun observeDetailSummary(userNo: String, schoolYear: String): Flow<PEDetailSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailScores(scores: List<PESubjectScoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailSummary(summary: PEDetailSummaryEntity)

    @Query("DELETE FROM pe_detail_scores WHERE userNo = :userNo AND schoolYear = :schoolYear")
    suspend fun deleteDetailScoresByYear(userNo: String, schoolYear: String)

    @Query("DELETE FROM pe_detail_summary WHERE userNo = :userNo AND schoolYear = :schoolYear")
    suspend fun deleteDetailSummaryByYear(userNo: String, schoolYear: String)

    @Query("DELETE FROM pe_detail_scores WHERE userNo = :userNo")
    suspend fun deleteScoresByUserNo(userNo: String)

    @Query("DELETE FROM pe_detail_summary WHERE userNo = :userNo")
    suspend fun deleteSummaryByUserNo(userNo: String)

    @Query("DELETE FROM pe_scores WHERE userNo = :userNo")
    suspend fun deleteYearScoresByUserNo(userNo: String)

    @Transaction
    suspend fun deletePeDataByUserNo(userNo: String) {
        deleteScoresByUserNo(userNo)
        deleteSummaryByUserNo(userNo)
        deleteYearScoresByUserNo(userNo)
    }

    @Transaction
    suspend fun replaceDetailByYear(
        userNo: String,
        schoolYear: String,
        summary: PEDetailSummaryEntity?,
        scores: List<PESubjectScoreEntity>
    ) {
        deleteDetailScoresByYear(userNo, schoolYear)
        deleteDetailSummaryByYear(userNo, schoolYear)
        summary?.let { insertDetailSummary(it) }
        insertDetailScores(scores)
    }
}
