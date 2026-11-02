package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.GradeDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.repository.GradeRepository

class RoomGradeRepository(
    private val gradeDao: GradeDao
) : GradeRepository {

    override fun observeGradesByUserNo(userNo: String): Flow<List<GradeCourse>> {
        if (userNo.isBlank()) return flowOf(emptyList())
        return gradeDao.observeGradesByUserNo(userNo).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllGradesByUserNo(userNo: String): List<GradeCourse> {
        if (userNo.isBlank()) return emptyList()
        return gradeDao.getAllGradesByUserNo(userNo).map { it.toDomain() }
    }

    override suspend fun replaceGradesByUserNoAndSemester(
        userNo: String,
        semester: String,
        grades: List<GradeCourse>
    ) {
        gradeDao.replaceByUserNoAndSemester(userNo, semester, grades.map { it.toEntity(userNo) })
    }
}
