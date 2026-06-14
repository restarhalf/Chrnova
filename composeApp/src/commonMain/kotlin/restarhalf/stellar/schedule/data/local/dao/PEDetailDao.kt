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
    @Query("SELECT * FROM pe_detail_scores WHERE schoolYear = :schoolYear")
    fun observeDetailScores(schoolYear: String): Flow<List<PESubjectScoreEntity>>

    @Query("SELECT * FROM pe_detail_summary WHERE schoolYear = :schoolYear LIMIT 1")
    fun observeDetailSummary(schoolYear: String): Flow<PEDetailSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailScores(scores: List<PESubjectScoreEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetailSummary(summary: PEDetailSummaryEntity)

    @Query("DELETE FROM pe_detail_scores WHERE schoolYear = :schoolYear")
    suspend fun deleteDetailScoresByYear(schoolYear: String)

    @Query("DELETE FROM pe_detail_summary WHERE schoolYear = :schoolYear")
    suspend fun deleteDetailSummaryByYear(schoolYear: String)

    @Query("DELETE FROM pe_detail_scores")
    suspend fun deleteAllScores()

    @Query("DELETE FROM pe_detail_summary")
    suspend fun deleteAllSummary()

    @Transaction
    suspend fun replaceDetailByYear(
        schoolYear: String,
        summary: PEDetailSummaryEntity?,
        scores: List<PESubjectScoreEntity>
    ) {
        deleteDetailScoresByYear(schoolYear)
        deleteDetailSummaryByYear(schoolYear)
        summary?.let { insertDetailSummary(it) }
        insertDetailScores(scores)
    }
}