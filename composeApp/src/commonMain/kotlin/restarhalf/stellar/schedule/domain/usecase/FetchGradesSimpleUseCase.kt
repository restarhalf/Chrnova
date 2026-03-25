package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.TermGradeReport

class FetchGradesSimpleUseCase(
    private val fetchGrades: FetchGradesUseCase,
) {
    suspend operator fun invoke(semester: String = ""): TermGradeReport {
        return fetchGrades(semester = semester)
    }
}