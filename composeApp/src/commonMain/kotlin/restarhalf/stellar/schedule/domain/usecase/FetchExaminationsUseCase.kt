package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository
import kotlinx.coroutines.flow.first

/**
 * 获取考试安排用例
 * 
 * 从教务系统获取考试安排，并保存到本地数据库。
 */
class FetchExaminationsUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
    private val repository: ExaminationRepository,
    private val auth: AuthPort,
) {
    /**
     * 获取考试安排
     * 
     * @param semester 学期
     * @param nameOrNumber 课程名称或编号筛选
     * @return 考试安排列表
     */
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
            if (e.isNetworkError()) throw e
            AppLogger.log("Fetch", "获取考试安排失败，刷新会话重试", e)
            authWorkflow.refreshSession()
            academic.fetchExaminations(semester = semester, nameOrNumber = nameOrNumber)
        }

        val userNo = try { auth.observeProfile().first().userNo } catch (_: Exception) { "" }
        val boundExams = exams.map { it.copy(userNo = userNo) }

        if (nameOrNumber.isBlank()) {
            repository.replaceExaminations(semester, boundExams)
        }

        return boundExams
    }
}
