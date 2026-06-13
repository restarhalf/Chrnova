package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 观察总周数用例
 * 
 * 获取学期总周数变化的响应式数据流。
 */
class ObserveTotalWeeksUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 观察总周数
     * 
     * @return 总周数Flow
     */
    operator fun invoke(): Flow<Int> = timetable.observeTotalWeeks()
}
