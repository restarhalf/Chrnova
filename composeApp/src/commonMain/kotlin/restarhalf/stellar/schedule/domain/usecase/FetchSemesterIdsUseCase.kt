package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

class FetchSemesterIdsUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
) {
    suspend operator fun invoke(): List<String> {
        val firstAttempt = runCatching { academic.fetchSemesterIds() }
        if (firstAttempt.isSuccess) return firstAttempt.getOrThrow()

        authWorkflow.logout()
        authWorkflow.ensureLoggedIn()
        return runCatching { academic.fetchSemesterIds() }.getOrElse { emptyList() }
    }
}
