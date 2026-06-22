package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.ExaminationDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * Room考试安排仓库实现类
 * 
 * 实现ExaminationRepository接口，负责考试安排数据的本地存储和查询。
 * 
 * @param examinationDao 考试安排DAO
 */
class RoomExaminationRepository(
    private val examinationDao: ExaminationDao
) : ExaminationRepository {

    /**
     * 观察所有考试安排
     * 
     * @return 考试安排列表Flow
     */
    override fun observeAllExaminations(): Flow<List<Examination>> {
        return examinationDao.observeAllExaminations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 按学号观察考试安排
     * 
     * @param userNo 学号
     * @return 考试安排列表Flow
     */
    override fun observeExaminationsByUserNo(userNo: String): Flow<List<Examination>> {
        return examinationDao.observeExaminationsByUserNo(userNo).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * 按ID观察单个考试安排
     * 
     * @param id 考试ID
     * @return 考试安排Flow
     */
    override fun observeExaminationById(id: Long): Flow<Examination?> {
        return examinationDao.observeExaminationById(id).map { entity ->
            entity?.toDomain()
        }
    }

    /**
     * 保存单个考试安排
     * 
     * @param examination 考试安排
     * @param semesterId 学期ID
     * @return 保存的行ID
     */
    override suspend fun saveExamination(examination: Examination, semesterId: String): Long {
        return examinationDao.insertExamination(examination.toEntity(semesterId))
    }

    /**
     * 删除单个考试安排
     * 
     * @param id 考试ID
     */
    override suspend fun deleteExamination(id: Long) {
        examinationDao.deleteExamination(id)
    }

    /**
     * 替换考试安排数据
     * 
     * @param semesterId 学期ID
     * @param examinations 考试安排列表
     */
    override suspend fun replaceExaminations(semesterId: String, examinations: List<Examination>) {
        examinationDao.deleteExaminationsBySemester(semesterId)
        if (examinations.isNotEmpty()) {
            examinationDao.insertExaminations(examinations.map { it.toEntity(semesterId) })
        }
    }

    /** 清除所有考试安排数据 */
    override suspend fun clearAll() {
        examinationDao.deleteAll()
    }

    /** 将未绑定学号的考试绑定到指定学号 */
    override suspend fun bindUnboundExaminations(userNo: String) {
        examinationDao.bindUnboundExaminations(userNo)
    }
}
