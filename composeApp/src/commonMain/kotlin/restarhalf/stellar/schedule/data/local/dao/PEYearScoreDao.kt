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
    @Query("SELECT * FROM pe_scores WHERE userNo = :userNo ORDER BY schoolYear DESC")
    fun observeAllScores(userNo: String): Flow<List<PEYearScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScores(scores: List<PEYearScoreEntity>)

    @Query("DELETE FROM pe_scores WHERE userNo = :userNo")
    suspend fun deleteByUserNo(userNo: String)

    @Transaction
    suspend fun replaceAll(userNo: String, scores: List<PEYearScoreEntity>) {
        deleteByUserNo(userNo)
        if (scores.isNotEmpty()) {
            insertScores(scores)
        }
    }
}
