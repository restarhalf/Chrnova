package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.policy.distinctExaminations
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class FetchExaminationsUseCase(
    private val authWorkflow: JwxtAuthWorkflowPort,
    private val academic: AcademicPort,
    private val repository: ExaminationRepository,
    private val auth: JwxtAuthPort,
    private val settings: TermSettingsPort,
) {
    
    suspend operator fun invoke(
        semester: String = "",
        nameOrNumber: String = ""
    ): List<Examination> {
        authWorkflow.ensureLoggedIn()

        val resolvedSemester = resolveSemester(semester)
        val resolvedNameOrNumber = resolveNameOrNumber(nameOrNumber)

        val exams = try {
            academic.fetchExaminations(
                semester = resolvedSemester,
                nameOrNumber = resolvedNameOrNumber
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (e.isNetworkError()) throw e
            AppLogger.log("Fetch", "获取考试安排失败，刷新会话重试", e)
            authWorkflow.refreshSession()
            academic.fetchExaminations(
                semester = resolvedSemester,
                nameOrNumber = resolvedNameOrNumber
            )
        }

        val userNo = try { auth.observeProfile().first().userNo } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            AppLogger.log("Fetch", "获取用户学号失败，使用空值", e)
            ""
        }
        val boundExams = exams.distinctExaminations().map { it.copy(userNo = userNo) }

        if (nameOrNumber.isBlank()) {
            repository.replaceExaminations(resolvedSemester, boundExams)
        }

        return boundExams
    }

    private suspend fun resolveSemester(semester: String): String {
        if (semester.isNotBlank()) return semester
        val selectedTerm = settings.observeSelectedTerm().first()
        if (selectedTerm.isNotBlank()) return selectedTerm
        return academic.fetchCurrentTermId()
    }

    private suspend fun resolveNameOrNumber(nameOrNumber: String): String {
        if (nameOrNumber.isNotBlank()) return nameOrNumber
        return try {
            auth.observeProfile().first().userNo
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            ""
        }
    }
}
