package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class ObserveAllExaminationsUseCase(
    private val repository: ExaminationRepository
) {
    operator fun invoke(): Flow<List<Examination>> {
        return repository.observeAllExaminations()
    }
}
