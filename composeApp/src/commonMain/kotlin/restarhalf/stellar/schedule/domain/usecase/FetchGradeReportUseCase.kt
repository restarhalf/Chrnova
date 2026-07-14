package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort

class FetchGradeReportUseCase(
    private val academic: AcademicPort,
) {
    suspend operator fun invoke(semester: String): TermGradeReport =
        academic.fetchGradeReport(semester = semester)
}
