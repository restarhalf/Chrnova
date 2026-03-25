package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.TimetablePort

class GetTotalWeeksUseCase(
    private val timetable: TimetablePort,
) {
    operator fun invoke(): Int = timetable.getTotalWeeks()
}
