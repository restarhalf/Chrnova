package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.PEDetailDao
import restarhalf.stellar.schedule.data.local.dao.PEYearScoreDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.PEDetailData
import restarhalf.stellar.schedule.domain.model.PEYearScore
import restarhalf.stellar.schedule.domain.repository.PERepository

class RoomPERepositoryImpl(
    private val peYearScoreDao: PEYearScoreDao,
    private val peDetailDao: PEDetailDao
) : PERepository {
    override fun observeAllScores(): Flow<List<PEYearScore>> {
        return peYearScoreDao.observeAllScores().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeDetailData(schoolYear: String): Flow<PEDetailData?> {
        return combine(
            peDetailDao.observeDetailSummary(schoolYear),
            peDetailDao.observeDetailScores(schoolYear)
        ) { summary, scores ->
            summary?.toDomain(scores.map { it.toDomain() })
        }
    }

    override suspend fun replaceScores(scores: List<PEYearScore>) {
        peYearScoreDao.replaceAll(scores.map { it.toEntity() })
    }

    override suspend fun saveDetailData(schoolYear: String, detail: PEDetailData) {
        val (summaries, scores) = detail.toEntity(schoolYear)
        peDetailDao.replaceDetailByYear(schoolYear, summaries.firstOrNull(), scores)
    }

    override suspend fun clearAll() {
        peDetailDao.deleteAllPeData()
    }
}
