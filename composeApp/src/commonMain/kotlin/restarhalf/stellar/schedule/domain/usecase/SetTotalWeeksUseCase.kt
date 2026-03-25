package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.TimetablePort

class SetTotalWeeksUseCase(
    private val timetable: TimetablePort,
) {
    operator fun invoke(weeks: Int) {
        timetable.setTotalWeeks(weeks)
    }
}
