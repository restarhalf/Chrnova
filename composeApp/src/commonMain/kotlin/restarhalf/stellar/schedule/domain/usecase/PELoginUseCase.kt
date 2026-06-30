package restarhalf.stellar.schedule.domain.usecase

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
}