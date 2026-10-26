package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class SaveExaminationUseCase(private val repository: ExaminationRepository) {
    
    suspend operator fun invoke(examination: Examination, semesterId: String): Long {
        return repository.saveExamination(examination, semesterId)
    }
}
