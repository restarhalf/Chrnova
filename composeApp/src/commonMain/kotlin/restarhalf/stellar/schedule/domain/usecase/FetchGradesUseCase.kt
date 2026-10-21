package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.time.SemesterUtils
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository


class FetchGradesUseCase(
    private val authWorkflow: JwxtAuthWorkflowPort,
    private val academic: AcademicPort,
    private val settings: TermSettingsPort,
    private val repository: GradeRepository,
    private val auth: JwxtAuthPort,
) {
    
    suspend operator fun invoke(semester: String = ""): TermGradeReport {
        
        suspend fun resolveSemester(): String {
            val selectedTerm = semester.ifBlank { settings.observeSelectedTerm().first() }
            return selectedTerm.ifBlank { academic.fetchCurrentTermId() }
        }

        
        suspend fun resolvePreviousSemesterOrNull(current: String): String? {
            val all = academic.fetchSemesterIds().filter { it.isNotBlank() }.distinct()
            if (all.isEmpty()) return null

            val sorted = all.sortedWith(SemesterUtils.comparator).asReversed()
            val idx = sorted.indexOf(current)
            if (idx == -1) return null
            return sorted.getOrNull(idx + 1)?.takeIf { it != current }
        }

        
        suspend fun fetchWithFallback(resolvedSemester: String): TermGradeReport {
            val current = academic.fetchGradeReport(semester = resolvedSemester)
            if (current.achievements.isNotEmpty()) return current

            val previous = resolvePreviousSemesterOrNull(resolvedSemester) ?: return current
            return academic.fetchGradeReport(semester = previous)
        }

        authWorkflow.ensureLoggedIn()


        val report = try {
            val resolvedSemester = resolveSemester()
            fetchWithFallback(resolvedSemester)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (e.isNetworkError()) throw e
            AppLogger.log("Fetch", "获取成绩失败，刷新会话重试", e)
            authWorkflow.refreshSession()
            val resolvedSemester = resolveSemester()
            fetchWithFallback(resolvedSemester)
        }


        if (report.achievements.isNotEmpty()) {
            val userNo = try {
                auth.observeProfile().first().userNo
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.log("Fetch", "获取用户学号失败，使用空值", e)
                ""
            }
            val sem = report.achievements.firstOrNull()?.semester ?: ""
            if (sem.isNotBlank()) {
                val gradesWithUserNo = report.achievements.map { it.copy(userNo = userNo) }
                repository.replaceGradesByUserNoAndSemester(userNo, sem, gradesWithUserNo)
            }
        }

        return report
    }
}