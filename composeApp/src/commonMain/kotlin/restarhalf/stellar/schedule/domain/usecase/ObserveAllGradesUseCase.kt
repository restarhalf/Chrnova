package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository

class ObserveAllGradesUseCase(
    private val repository: GradeRepository,
    private val auth: JwxtAuthPort,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<GradeCourse>> {
        return auth.observeProfile().map { it.userNo }.distinctUntilChanged()
            .flatMapLatest { userNo ->
                if (userNo.isBlank()) {
                    flowOf(emptyList())
                } else {
                    repository.observeGradesByUserNo(userNo)
                        .onStart { emit(emptyList()) }
                }
            }
    }
}
