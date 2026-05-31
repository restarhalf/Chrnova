package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.ExaminationDao
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

    override suspend fun replaceExaminations(semesterId: String, examinations: List<Examination>) {
        examinationDao.deleteExaminationsBySemester(semesterId)
        if (examinations.isNotEmpty()) {
            examinationDao.insertExaminations(examinations.map { it.toEntity(semesterId) })
        }
    }

    override suspend fun clearAll() {
        examinationDao.deleteAll()
    }
}
