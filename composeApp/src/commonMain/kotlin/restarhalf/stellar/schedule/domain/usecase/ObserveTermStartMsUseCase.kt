package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.TimetablePort

class ObserveTermStartMsUseCase(
    private val timetable: TimetablePort,
) {
    operator fun invoke(): Flow<Long> = timetable.observeTermStartMs()
}
