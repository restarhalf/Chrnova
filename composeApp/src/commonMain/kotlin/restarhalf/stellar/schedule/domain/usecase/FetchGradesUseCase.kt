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

        suspend fun fetchWithFallback(
            resolvedSemester: String,
        ): Pair<TermGradeReport, List<String>> {
            val current = academic.fetchGradeReport(semester = resolvedSemester)
            if (current.achievements.isNotEmpty()) return current to listOf(resolvedSemester)

            val previous = resolvePreviousSemesterOrNull(resolvedSemester)
                ?: return current to listOf(resolvedSemester)
            return academic.fetchGradeReport(semester = previous) to
                listOf(resolvedSemester, previous)
        }

        authWorkflow.ensureLoggedIn()

        val session = auth.getSessionSnapshot()
        require(session.userNo.isNotBlank()) { "请先登录" }
        val (report, queriedSemesters) = try {
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

        val sessionUnchanged = auth.getSessionSnapshot() == session
        if (report.achievements.isNotEmpty()) {
            require(sessionUnchanged) { "登录状态已变化，请重试" }
            val fallbackSemester = report.achievements.first().semester
            report.achievements
                .map { it.copy(userNo = session.userNo) }
                .groupBy { it.semester.ifBlank { fallbackSemester } }
                .forEach { (semester, grades) ->
                    if (semester.isNotBlank()) {
                        repository.replaceGradesByUserNoAndSemester(
                            session.userNo,
                            semester,
                            grades,
                        )
                    }
                }
        } else if (sessionUnchanged) {
            queriedSemesters.filter { it.isNotBlank() }.forEach { semester ->
                repository.replaceGradesByUserNoAndSemester(
                    session.userNo,
                    semester,
                    emptyList(),
                )
            }
        }

        return report
    }
}
