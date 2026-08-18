package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.JwxtAuthWorkflowPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 获取考试安排用例
 *
 * 从教务系统获取考试安排，并保存到本地数据库。
 * 支持学期解析：如果未指定学期，会依次尝试选中学期、当前学期。
 * 同时传递学号作为nameOrNumber参数进行筛选。
 */
class FetchExaminationsUseCase(
    private val authWorkflow: JwxtAuthWorkflowPort,
    private val academic: AcademicPort,
    private val repository: ExaminationRepository,
    private val auth: JwxtAuthPort,
    private val settings: SettingsPort,
) {
    /**
     * 获取考试安排
     *
     * @param semester 学期ID，为空时自动解析（选中学期 → 当前学期）
     * @param nameOrNumber 课程名称或编号筛选，为空时自动使用当前用户学号
     * @return 考试安排列表
     */
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
        val boundExams = exams.map { it.copy(userNo = userNo) }

        if (nameOrNumber.isBlank()) {
            repository.replaceExaminations(resolvedSemester, boundExams)
        }

        return boundExams
    }

    /**
     * 解析学期ID
     *
     * 优先级：参数传入 > 选中学期 > 当前学期
     */
    private suspend fun resolveSemester(semester: String): String {
        if (semester.isNotBlank()) return semester
        val selectedTerm = settings.observeSelectedTerm().first()
        if (selectedTerm.isNotBlank()) return selectedTerm
        return academic.fetchCurrentTermId()
    }

    /**
     * 解析nameOrNumber参数
     *
     * 如果未指定，自动使用当前用户的学号
     */
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
