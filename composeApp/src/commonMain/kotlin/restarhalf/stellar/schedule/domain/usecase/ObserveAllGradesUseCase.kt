package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.domain.model.GradeCourse
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.repository.GradeRepository

/**
 * 观察所有成绩用例
 *
 * 按当前用户学号过滤成绩的响应式数据流。
 */
class ObserveAllGradesUseCase(
    private val repository: GradeRepository,
    private val auth: JwxtAuthPort,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<GradeCourse>> {
        return auth.observeProfile().map { it.userNo }.distinctUntilChanged()
            .flatMapLatest { userNo ->
                if (userNo.isNotBlank()) {
                    repository.observeGradesByUserNo(userNo)
                } else {
                    repository.observeAllGrades()
                }
            }
    }
}
