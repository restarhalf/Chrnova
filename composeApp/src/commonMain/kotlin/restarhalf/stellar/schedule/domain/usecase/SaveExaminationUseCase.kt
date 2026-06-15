package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 保存考试安排用例
 * 
 * 将考试安排数据保存到本地数据库。
 */
class SaveExaminationUseCase(private val repository: ExaminationRepository) {
    /**
     * 保存考试安排
     * 
     * @param examination 考试安排数据
     * @param semesterId 学期ID
     * @return 保存后的行ID
     */
    suspend operator fun invoke(examination: Examination, semesterId: String): Long {
        return repository.saveExamination(examination, semesterId)
    }
}
