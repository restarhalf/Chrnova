package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.data.remote.PELoginResponse
import restarhalf.stellar.schedule.data.repository.PERepository
import restarhalf.stellar.schedule.domain.port.PEAuthPort

/**
 * 体育系统登录用例
 */
class PELoginUseCase(
    private val repository: PERepository,
    private val peAuth: PEAuthPort,
) {
    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean = peAuth.getToken() != null

    /**
     * 用户登录
     */
    suspend operator fun invoke(username: String, password: String): PELoginResponse {
        return repository.login(username, password)
    }

    /**
     * 使用存储的凭据自动重新登录
     *
     * @return 登录结果，失败返回null
     */
    suspend fun autoLogin(): PELoginResponse? {
        val username = peAuth.getUsername() ?: return null
        val password = peAuth.getPassword() ?: return null
        return try {
            repository.login(username, password)
        } catch (e: Exception) {
            AppLogger.log("PE", "自动重试失败", e)
            null
        }
    }
}