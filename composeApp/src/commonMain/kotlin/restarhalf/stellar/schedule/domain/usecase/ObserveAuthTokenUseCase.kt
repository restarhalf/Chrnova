package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.AuthPort

/**
 * 观察认证令牌用例
 * 
 * 获取认证令牌的响应式数据流。
 */
class ObserveAuthTokenUseCase(
    private val auth: AuthPort,
) {
    /**
     * 观察认证令牌
     * 
     * @return 令牌Flow
     */
    operator fun invoke(): Flow<String> = auth.observeToken()
}
