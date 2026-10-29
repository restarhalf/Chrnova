package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class RepairExaminationSemestersUseCase(
    private val examinationRepository: ExaminationRepository,
    private val academic: AcademicPort,
) {

    suspend operator fun invoke() {
        try {
            val currentSemesterId = academic.fetchCurrentTermId()
            if (currentSemesterId.isNotBlank()) {
                examinationRepository.fixInvalidSemesterIds(currentSemesterId)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AppLogger.log("Repair", "修复考试学期ID失败", e)
        }
    }
}
