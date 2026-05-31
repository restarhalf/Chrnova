package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.GradeDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.repository.GradeRepository

class RoomGradeRepository(
    private val gradeDao: GradeDao
) : GradeRepository {

    override fun observeAllGrades(): Flow<List<GradeCourse>> {
        return gradeDao.observeAllGrades().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun replaceGrades(semester: String, grades: List<GradeCourse>) {
        gradeDao.deleteGradesBySemester(semester)
        if (grades.isNotEmpty()) {
            gradeDao.insertGrades(grades.map { it.toEntity() })
        }
    }

    override suspend fun clearAll() {
        gradeDao.deleteAll()
    }
}
