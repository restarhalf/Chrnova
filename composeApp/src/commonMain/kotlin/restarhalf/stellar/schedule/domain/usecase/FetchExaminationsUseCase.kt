package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 获取考试安排用例
 * 
 * 从教务系统获取考试安排，并保存到本地数据库。
 */
class FetchExaminationsUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
    private val repository: ExaminationRepository,
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

        // 获取考试安排，如果失败则刷新会话后重试
        val exams = try {
            academic.fetchExaminations(
                semester = semester,
                nameOrNumber = nameOrNumber
            )
        } catch (e: Exception) {
            authWorkflow.refreshSession()
            academic.fetchExaminations(semester = semester, nameOrNumber = nameOrNumber)
        }

        // 如果是全量查询，保存到本地数据库
        if (nameOrNumber.isBlank()) {
            repository.replaceExaminations(semester, exams)
        }

        return exams
    }
}
