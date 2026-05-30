package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class ObserveExaminationsUseCase(
    private val repository: ExaminationRepository
) {
    operator fun invoke(semesterId: String): Flow<List<Examination>> {
        return repository.observeExaminations(semesterId)
    }
}
