package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Examination

class FetchExaminationsSimpleUseCase(
    private val fetchExaminations: FetchExaminationsUseCase,
) {
    suspend operator fun invoke(
        semester: String = "",
        nameOrNumber: String = ""
    ): List<Examination> {
        return fetchExaminations(semester = semester, nameOrNumber = nameOrNumber)
    }
}
