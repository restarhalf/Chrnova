package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AuthWorkflowPort

/**
 * 确保已登录用例
 * 
 * 确保用户已登录，如果未登录则尝试使用保存的凭据登录。
 */
class EnsureLoggedInUseCase(
    private val authWorkflow: AuthWorkflowPort,
) {
    /**
     * 执行确保已登录
     */
    suspend operator fun invoke() {
        authWorkflow.ensureLoggedIn()
    }
}
