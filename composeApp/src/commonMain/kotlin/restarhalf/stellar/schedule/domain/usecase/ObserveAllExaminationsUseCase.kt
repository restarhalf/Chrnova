package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.AuthPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

/**
 * 观察所有考试安排用例
 *
 * 按当前用户学号过滤考试安排的响应式数据流。
 */
class ObserveAllExaminationsUseCase(
    private val repository: ExaminationRepository,
    private val auth: AuthPort,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Examination>> {
        return auth.observeProfile().map { it.userNo }.distinctUntilChanged()
            .flatMapLatest { userNo ->
                if (userNo.isNotBlank()) {
                    repository.observeExaminationsByUserNo(userNo)
                } else {
                    repository.observeAllExaminations()
                }
            }
    }
}
