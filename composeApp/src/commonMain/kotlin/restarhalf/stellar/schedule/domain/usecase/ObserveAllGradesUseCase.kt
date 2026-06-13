package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.repository.GradeRepository

/**
 * 观察所有成绩用例
 * 
 * 获取所有成绩的响应式数据流。
 */
class ObserveAllGradesUseCase(
    private val repository: GradeRepository
) {
    /**
     * 观察所有成绩
     * 
     * @return 成绩列表Flow
     */
    operator fun invoke(): Flow<List<GradeCourse>> {
        return repository.observeAllGrades()
    }
}
