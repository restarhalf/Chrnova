package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.ExaminationDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class RoomExaminationRepository(
    private val examinationDao: ExaminationDao,
    private val auth: JwxtAuthPort,
) : ExaminationRepository {

    override suspend fun getAllExaminationsOnce(): List<Examination> {
        val userNo = auth.observeProfile().first().userNo
        if (userNo.isBlank()) return emptyList()
        return examinationDao.getAllExaminationsByUserNoOnce(userNo).map { it.toDomain() }
    }

    override fun observeExaminationsByUserNo(userNo: String): Flow<List<Examination>> {
        if (userNo.isBlank()) return flowOf(emptyList())
        return examinationDao.observeExaminationsByUserNo(userNo).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeExaminationById(id: Long): Flow<Examination?> {
        return combine(
            examinationDao.observeExaminationById(id),
            auth.observeProfile().map { it.userNo },
        ) { entity, userNo ->
            entity?.toDomain()?.takeIf { exam ->
                userNo.isNotBlank() && exam.userNo == userNo
            }
        }
    }

    override suspend fun saveExamination(examination: Examination, semesterId: String): Long {
        return examinationDao.insertExamination(examination.toEntity(semesterId))
    }

    override suspend fun deleteExamination(id: Long) {
        examinationDao.deleteExamination(id)
    }

    override suspend fun replaceExaminations(semesterId: String, examinations: List<Examination>) {
        examinationDao.replaceBySemester(semesterId, examinations.map { it.toEntity(semesterId) })
    }

    override suspend fun clearAll() {
        examinationDao.deleteAll()
    }

    override suspend fun bindUnboundExaminations(userNo: String) {
        examinationDao.bindUnboundExaminations(userNo)
    }

    override suspend fun fixInvalidSemesterIds(correctSemesterId: String) {
        examinationDao.updateSemesterId("", correctSemesterId)
        examinationDao.updateSemesterId("manual", correctSemesterId)
    }
}
