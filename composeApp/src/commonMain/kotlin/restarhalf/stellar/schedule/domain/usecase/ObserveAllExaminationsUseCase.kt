package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.ExaminationRepository

class ObserveAllExaminationsUseCase(
    private val repository: ExaminationRepository,
    private val auth: JwxtAuthPort,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Examination>> {
        return auth.observeProfile().map { it.userNo }.distinctUntilChanged()
            .flatMapLatest { userNo ->
                if (userNo.isBlank()) {
                    flowOf(emptyList())
                } else {
                    repository.observeExaminationsByUserNo(userNo)
                        .onStart { emit(emptyList()) }
                }
            }
    }
}
