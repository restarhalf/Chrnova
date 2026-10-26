package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class DeleteExaminationUseCase(private val repository: ExaminationRepository) {
    
    suspend operator fun invoke(id: Long) {
        repository.deleteExamination(id)
    }
}
