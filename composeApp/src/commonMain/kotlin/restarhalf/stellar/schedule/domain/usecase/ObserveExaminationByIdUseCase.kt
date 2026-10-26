package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class ObserveExaminationByIdUseCase(private val repository: ExaminationRepository) {

    operator fun invoke(examinationId: Long): Flow<Examination?> =
        repository.observeExaminationById(examinationId)
}
