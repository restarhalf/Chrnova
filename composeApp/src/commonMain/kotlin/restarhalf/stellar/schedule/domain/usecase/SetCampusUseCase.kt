package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.TimetablePort

class SetCampusUseCase(
    private val timetable: TimetablePort,
) {
    operator fun invoke(campus: Campus) {
        timetable.setCampus(campus)
    }
}
