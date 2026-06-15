package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 删除考试安排用例
 * 
 * 从本地数据库删除指定考试安排。
 */
class DeleteExaminationUseCase(private val repository: ExaminationRepository) {
    /**
     * 删除考试安排
     * 
     * @param id 要删除的考试ID
     */
    suspend operator fun invoke(id: Long) {
        repository.deleteExamination(id)
    }
}
