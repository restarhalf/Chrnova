package restarhalf.stellar.schedule.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.local.entity.PEYearScoreEntity

@Dao
interface PEYearScoreDao {
    @Query("SELECT * FROM pe_scores ORDER BY schoolYear DESC")
    fun observeAllScores(): Flow<List<PEYearScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<PEYearScoreEntity>)

    @Query("DELETE FROM pe_scores")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(scores: List<PEYearScoreEntity>) {
        deleteAll()
        if (scores.isNotEmpty()) {
            insertScores(scores)
        }
    }
}