package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort

class FetchGradesUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
    private val settings: SettingsPort,
) {
    suspend operator fun invoke(semester: String = ""): TermGradeReport {
        fun parseSemesterKey(id: String): Triple<Int, Int, Int>? {
            val parts = id.trim().split("-")
            if (parts.size < 3) return null
            val y1 = parts[0].toIntOrNull() ?: return null
            val y2 = parts[1].toIntOrNull() ?: return null
            val t = parts[2].toIntOrNull() ?: return null
            return Triple(y1, y2, t)
        }

        suspend fun resolveSemester(): String {
            val selectedTerm = semester.ifBlank { settings.observeSelectedTerm().first() }
            return selectedTerm.ifBlank { academic.fetchCurrentTermId() }
        }

        suspend fun resolvePreviousSemesterOrNull(current: String): String? {
            val all = academic.fetchSemesterIds().filter { it.isNotBlank() }.distinct()
            if (all.isEmpty()) return null

            val comparator = Comparator<String> { a, b ->
                val ka = parseSemesterKey(a)
                val kb = parseSemesterKey(b)
                when {
                    ka != null && kb != null -> {
                        if (ka.first != kb.first) return@Comparator ka.first.compareTo(kb.first)
                        if (ka.second != kb.second) return@Comparator ka.second.compareTo(kb.second)
                        ka.third.compareTo(kb.third)
                    }

                    ka != null -> 1
                    kb != null -> -1
                    else -> a.compareTo(b)
                }
            }

            val sorted = all.sortedWith(comparator).asReversed()
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

        val firstAttempt = runCatching {
            val resolvedSemester = resolveSemester()
            fetchWithFallback(resolvedSemester)
        }
        if (firstAttempt.isSuccess) return firstAttempt.getOrThrow()

        authWorkflow.logout()
        authWorkflow.ensureLoggedIn()
        val resolvedSemester = resolveSemester()
        return fetchWithFallback(resolvedSemester)
    }
}