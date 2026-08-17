package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.PEAuthWorkflowPort

/**
 * 体育系统登录用例
 *
 * 封装体育系统登录的业务逻辑，调用认证工作流端口执行登录操作。
 */
class PELoginUseCase(
    private val peAuthWorkflow: PEAuthWorkflowPort,
) {
    /**
     * 执行登录
     *
     * @param username 用户名
     * @param password 密码
     */
    suspend operator fun invoke(
        username: String,
        password: String,
    ) {
        try {
            peAuthWorkflow.login(
                username = username,
                password = password,
            )
        } catch (e: Exception) {
            AppLogger.log("PEAuth", "登录失败 username=$username", e)
            throw e
        }
    }
}
