package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 观察所有考试安排用例
 * 
 * 获取所有考试安排的响应式数据流。
 */
class ObserveAllExaminationsUseCase(
    private val repository: ExaminationRepository
) {
    /**
     * 观察所有考试安排
     * 
     * @return 考试安排列表Flow
     */
    operator fun invoke(): Flow<List<Examination>> {
        return repository.observeAllExaminations()
    }
}
