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

    /**
     * 观察所有成绩
     * 
     * @return 成绩列表Flow
     */
    override fun observeAllGrades(): Flow<List<GradeCourse>> {
        return gradeDao.observeAllGrades().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 替换成绩数据
     * 
     * @param semester 学期
     * @param grades 成绩列表
     */
    override suspend fun replaceGrades(semester: String, grades: List<GradeCourse>) {
        gradeDao.deleteGradesBySemester(semester)
        if (grades.isNotEmpty()) {
            gradeDao.insertGrades(grades.map { it.toEntity() })
        }
    }

    /** 清除所有成绩数据 */
    override suspend fun clearAll() {
        gradeDao.deleteAll()
    }
}
