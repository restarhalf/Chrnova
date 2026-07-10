package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.GradeDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.repository.GradeRepository

/**
 * Room成绩仓库实现类
 * 
 * 实现GradeRepository接口，负责成绩数据的本地存储和查询。
 * 
 * @param gradeDao 成绩DAO
 */
class RoomGradeRepository(
    private val gradeDao: GradeDao
) : GradeRepository {

    override fun observeAllGrades(): Flow<List<GradeCourse>> {
        return gradeDao.observeAllGrades().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeGradesByUserNo(userNo: String): Flow<List<GradeCourse>> {
        return gradeDao.observeGradesByUserNo(userNo).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun replaceGrades(semester: String, grades: List<GradeCourse>) {
        gradeDao.replaceBySemester(semester, grades.map { it.toEntity() })
    }

    override suspend fun replaceGradesByUserNoAndSemester(
        userNo: String,
        semester: String,
        grades: List<GradeCourse>
    ) {
        gradeDao.replaceByUserNoAndSemester(userNo, semester, grades.map { it.toEntity(userNo) })
    }

    override suspend fun clearAll() {
        gradeDao.deleteAll()
    }
}
