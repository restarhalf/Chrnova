package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AcademicPort
import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

/**
 * 获取学期ID列表用例
 * 
 * 从教务系统获取所有可用的学期ID列表。
 */
class FetchSemesterIdsUseCase(
    private val authWorkflow: AuthWorkflowPort,
    private val academic: AcademicPort,
) {
    /**
     * 获取学期ID列表
     * 
     * @return 学期ID列表
     */
    suspend operator fun invoke(): List<String> {
        // 尝试获取，如果失败则刷新会话后重试
        val firstAttempt = runCatching { academic.fetchSemesterIds() }
        if (firstAttempt.isSuccess) return firstAttempt.getOrThrow()

        authWorkflow.refreshSession()
        return runCatching { academic.fetchSemesterIds() }.getOrElse { emptyList() }
    }
}
