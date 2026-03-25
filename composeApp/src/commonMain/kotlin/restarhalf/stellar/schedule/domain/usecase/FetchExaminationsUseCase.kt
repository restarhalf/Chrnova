package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

class FetchExaminationsUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
) {
    suspend operator fun invoke(
        semester: String = "",
        nameOrNumber: String = ""
    ): List<Examination> {
        authWorkflow.ensureLoggedIn()

        val firstAttempt = runCatching {
            academic.fetchExaminations(
                semester = semester,
                nameOrNumber = nameOrNumber
            )
        }
        if (firstAttempt.isSuccess) return firstAttempt.getOrThrow()

        authWorkflow.logout()
        authWorkflow.ensureLoggedIn()
        return academic.fetchExaminations(semester = semester, nameOrNumber = nameOrNumber)
    }
}
