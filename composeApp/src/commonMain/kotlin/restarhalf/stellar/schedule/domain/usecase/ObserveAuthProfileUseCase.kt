package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.AuthProfile
import restarhalf.stellar.schedule.domain.port.AuthPort

/**
 * 观察用户档案用例
 * 
 * 获取用户档案信息的响应式数据流。
 */
class ObserveAuthProfileUseCase(
    private val auth: AuthPort,
) {
    /**
     * 观察用户档案
     * 
     * @return 用户档案Flow
     */
    operator fun invoke(): Flow<AuthProfile> = auth.observeProfile()
}
