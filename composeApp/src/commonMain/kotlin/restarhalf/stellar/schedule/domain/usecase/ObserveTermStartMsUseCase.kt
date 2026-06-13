package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 观察学期开始时间用例
 * 
 * 获取学期开始时间变化的响应式数据流。
 */
class ObserveTermStartMsUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 观察学期开始时间
     * 
     * @return 时间戳Flow（毫秒）
     */
    operator fun invoke(): Flow<Long> = timetable.observeTermStartMs()
}
