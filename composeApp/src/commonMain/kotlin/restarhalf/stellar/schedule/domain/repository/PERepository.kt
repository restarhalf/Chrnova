package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.PEDetailData
import restarhalf.stellar.schedule.domain.model.PEYearScore

interface PERepository {

    fun observeAllScores(): Flow<List<PEYearScore>>

    fun observeDetailData(schoolYear: String): Flow<PEDetailData?>

    suspend fun replaceScores(scores: List<PEYearScore>)

    suspend fun saveDetailData(schoolYear: String, detail: PEDetailData)

    suspend fun clearAll()
}
