package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.TimetablePort

class GetCampusTimetableUseCase(
    private val timetable: TimetablePort,
) {
    operator fun invoke(campus: Campus): List<TimetableSlot> = timetable.getCampusTimetable(campus)
}
