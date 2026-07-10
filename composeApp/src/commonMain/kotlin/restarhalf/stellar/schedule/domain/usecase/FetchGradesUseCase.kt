package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.TermGradeReport
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository

/**
 * 获取成绩用例
 *
 * 从教务系统获取成绩报告，并保存到本地数据库。
 * 支持学期回退：如果当前学期没有成绩，会尝试获取上一学期的成绩。
 */
class FetchGradesUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
    private val settings: SettingsPort,
    private val repository: GradeRepository,
    private val auth: AuthPort,
) {
    /**
     * 获取成绩报告
     * 
     * @param semester 学期，为空时使用当前选中学期
     * @return 学期成绩报告
     */
    suspend operator fun invoke(semester: String = ""): TermGradeReport {
        /**
         * 解析学期ID为可比较的三元组
         */
        fun parseSemesterKey(id: String): Triple<Int, Int, Int>? {
            val parts = id.trim().split("-")
            if (parts.size < 3) return null
            val y1 = parts[0].toIntOrNull() ?: return null
            val y2 = parts[1].toIntOrNull() ?: return null
            val t = parts[2].toIntOrNull() ?: return null
            return Triple(y1, y2, t)
        }

        /**
         * 解析要查询的学期
         */
        suspend fun resolveSemester(): String {
            val selectedTerm = semester.ifBlank { settings.observeSelectedTerm().first() }
            return selectedTerm.ifBlank { academic.fetchCurrentTermId() }
        }

        /**
         * 解析上一学期（用于回退）
         */
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

        /**
         * 带回退的成绩获取
         */
        suspend fun fetchWithFallback(resolvedSemester: String): TermGradeReport {
            val current = academic.fetchGradeReport(semester = resolvedSemester)
            if (current.achievements.isNotEmpty()) return current

            val previous = resolvePreviousSemesterOrNull(resolvedSemester) ?: return current
            return academic.fetchGradeReport(semester = previous)
        }

        authWorkflow.ensureLoggedIn()

        // 获取成绩，如果失败则刷新会话后重试
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

        // 保存到本地数据库
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