package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.data.repository.PERoomRepository
import restarhalf.stellar.schedule.domain.port.PEAuthPort

/**
 * 体育系统登出用例
 */
class PELogoutUseCase(
    private val peAuth: PEAuthPort,
    private val roomRepository: PERoomRepository? = null,
) {
    /**
     * 用户登出
     */
    suspend operator fun invoke() {
        peAuth.clearAll()
        roomRepository?.clearAll()
    }
}