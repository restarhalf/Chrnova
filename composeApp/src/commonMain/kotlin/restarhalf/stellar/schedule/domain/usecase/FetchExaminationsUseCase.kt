package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class FetchExaminationsUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
    private val repository: ExaminationRepository,
) {
    suspend operator fun invoke(
        semester: String = "",
        nameOrNumber: String = ""
    ): List<Examination> {
        authWorkflow.ensureLoggedIn()

        val exams = try {
            academic.fetchExaminations(
                semester = semester,
                nameOrNumber = nameOrNumber
            )
        } catch (e: Exception) {
            authWorkflow.logout()
            authWorkflow.ensureLoggedIn()
            academic.fetchExaminations(semester = semester, nameOrNumber = nameOrNumber)
        }

        if (nameOrNumber.isBlank()) {
            repository.replaceExaminations(semester, exams)
        }

        return exams
    }
}
