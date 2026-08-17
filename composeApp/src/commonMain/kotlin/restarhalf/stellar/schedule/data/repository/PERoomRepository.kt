package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.PEDetailDao
import restarhalf.stellar.schedule.data.local.dao.PEYearScoreDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.data.remote.PEDetailData
import restarhalf.stellar.schedule.data.remote.PEYearScore

class PERoomRepository(
    private val peYearScoreDao: PEYearScoreDao,
    private val peDetailDao: PEDetailDao
) {
    fun observeAllScores(): Flow<List<PEYearScore>> {
        return peYearScoreDao.observeAllScores().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeDetailData(schoolYear: String): Flow<PEDetailData?> {
        return combine(
            peDetailDao.observeDetailSummary(schoolYear),
            peDetailDao.observeDetailScores(schoolYear)
        ) { summary, scores ->
            summary?.toDomain(scores.map { it.toDomain() })
        }
    }

    suspend fun replaceScores(scores: List<PEYearScore>) {
        peYearScoreDao.replaceAll(scores.map { it.toEntity() })
    }

    suspend fun saveDetailData(schoolYear: String, detail: PEDetailData) {
        val (summaries, scores) = detail.toEntity(schoolYear)
        peDetailDao.replaceDetailByYear(schoolYear, summaries.firstOrNull(), scores)
    }

    suspend fun clearAll() {
        peDetailDao.deleteAllPeData()
    }
}
