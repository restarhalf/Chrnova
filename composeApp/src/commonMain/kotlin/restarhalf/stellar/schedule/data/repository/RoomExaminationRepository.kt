package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.ExaminationDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class RoomExaminationRepository(
    private val examinationDao: ExaminationDao
) : ExaminationRepository {

    override fun observeAllExaminations(): Flow<List<Examination>> {
        return examinationDao.observeAllExaminations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllExaminationsOnce(): List<Examination> {
        return examinationDao.getAllExaminationsOnce().map { it.toDomain() }
    }

    override fun observeExaminationsByUserNo(userNo: String): Flow<List<Examination>> {
        return examinationDao.observeExaminationsByUserNo(userNo).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeExaminationById(id: Long): Flow<Examination?> {
        return examinationDao.observeExaminationById(id).map { entity ->
            entity?.toDomain()
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
