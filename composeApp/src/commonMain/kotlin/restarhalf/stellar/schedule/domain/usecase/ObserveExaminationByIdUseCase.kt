package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 根据ID观察考试安排用例
 * 
 * 获取指定考试安排的响应式数据流。
 */
class ObserveExaminationByIdUseCase(
    private val repository: ExaminationRepository,
) {
    /**
     * 根据ID观察考试安排
     * 
     * @param id 考试ID
     * @return 考试安排Flow
     */
    operator fun invoke(id: Long): Flow<Examination?> = repository.observeExaminationById(id)
}
