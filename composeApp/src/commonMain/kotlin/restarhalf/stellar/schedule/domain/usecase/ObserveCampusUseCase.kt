package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.TimetablePort

class ObserveCampusUseCase(
    private val timetable: TimetablePort,
) {
    operator fun invoke(): Flow<Campus> = timetable.observeCampus()
}
