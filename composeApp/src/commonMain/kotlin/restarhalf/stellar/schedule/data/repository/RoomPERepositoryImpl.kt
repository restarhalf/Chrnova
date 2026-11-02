package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.PEDetailDao
import restarhalf.stellar.schedule.data.local.dao.PEYearScoreDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.PEDetailData
import restarhalf.stellar.schedule.domain.model.PEYearScore
import restarhalf.stellar.schedule.domain.port.PEAuthPort
import restarhalf.stellar.schedule.domain.repository.PERepository

@OptIn(ExperimentalCoroutinesApi::class)
class RoomPERepositoryImpl(
    private val peYearScoreDao: PEYearScoreDao,
    private val peDetailDao: PEDetailDao,
    private val auth: PEAuthPort,
) : PERepository {

    override fun observeAllScores(): Flow<List<PEYearScore>> =
        auth.observeUserId().flatMapLatest { userNo ->
            if (userNo.isBlank()) {
                flowOf(emptyList())
            } else {
                peYearScoreDao.observeAllScores(userNo).map { entities ->
                    entities.map { it.toDomain() }
                }
            }
        }

    override fun observeDetailData(schoolYear: String): Flow<PEDetailData?> =
        auth.observeUserId().flatMapLatest { userNo ->
            if (userNo.isBlank()) {
                flowOf(null)
            } else {
                combine(
                    peDetailDao.observeDetailSummary(userNo, schoolYear),
                    peDetailDao.observeDetailScores(userNo, schoolYear)
                ) { summary, scores ->
                    summary?.toDomain(scores.map { it.toDomain() })
                }
            }
        }

    override suspend fun replaceScores(scores: List<PEYearScore>) {
        val userNo = auth.observeUserId().first()
        if (userNo.isBlank()) return
        peYearScoreDao.replaceAll(userNo, scores.map { it.toEntity(userNo) })
    }

    override suspend fun saveDetailData(schoolYear: String, detail: PEDetailData) {
        val userNo = auth.observeUserId().first()
        if (userNo.isBlank()) return
        val (summaries, scores) = detail.toEntity(userNo, schoolYear)
        peDetailDao.replaceDetailByYear(userNo, schoolYear, summaries.firstOrNull(), scores)
    }

    override suspend fun clearUser(userId: String) {
        if (userId.isBlank()) return
        peDetailDao.deletePeDataByUserNo(userId)
    }
}
