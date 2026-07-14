package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AcademicPort

class GetCurrentTermIdUseCase(
    private val academic: AcademicPort,
) {
    suspend operator fun invoke(): String = academic.fetchCurrentTermId()
}
