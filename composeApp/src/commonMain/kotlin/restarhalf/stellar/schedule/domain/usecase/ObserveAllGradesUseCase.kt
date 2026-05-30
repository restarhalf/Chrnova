package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.repository.GradeRepository

class ObserveAllGradesUseCase(
    private val repository: GradeRepository
) {
    operator fun invoke(): Flow<List<GradeCourse>> {
        return repository.observeAllGrades()
    }
}
