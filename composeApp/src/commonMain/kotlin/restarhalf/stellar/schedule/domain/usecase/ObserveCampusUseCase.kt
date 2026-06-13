package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 观察校区用例
 * 
 * 获取校区变化的响应式数据流。
 */
class ObserveCampusUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 观察校区
     * 
     * @return 校区Flow
     */
    operator fun invoke(): Flow<Campus> = timetable.observeCampus()
}
