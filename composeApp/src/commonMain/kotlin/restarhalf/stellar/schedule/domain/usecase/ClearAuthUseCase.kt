package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.AuthPort

/**
 * 清除认证用例
 * 
 * 清除用户的认证信息，执行登出操作。
 */
class ClearAuthUseCase(
    private val auth: AuthPort,
) {
    /**
     * 执行清除认证
     */
    operator fun invoke() {
        auth.clear()
    }
}
