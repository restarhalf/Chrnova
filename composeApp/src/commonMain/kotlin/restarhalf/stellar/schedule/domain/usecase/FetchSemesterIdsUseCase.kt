package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.error.isNetworkError
import restarhalf.stellar.schedule.core.log.AppLogger
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
        val firstAttempt = runCatching { academic.fetchSemesterIds() }
        if (firstAttempt.isSuccess) return firstAttempt.getOrThrow()

        val ex = firstAttempt.exceptionOrNull()
        if (ex?.isNetworkError() == true) {
            AppLogger.log("Fetch", "获取学期列表网络错误", ex)
            return emptyList()
        }

        ex?.let { AppLogger.log("Fetch", "获取学期列表失败，刷新会话重试", it) }
        authWorkflow.refreshSession()
        return runCatching { academic.fetchSemesterIds() }
            .onFailure { AppLogger.log("Fetch", "获取学期列表重试失败", it) }
            .getOrElse { emptyList() }
    }
}
