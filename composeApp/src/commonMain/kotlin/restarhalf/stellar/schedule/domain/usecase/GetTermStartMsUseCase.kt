package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.TimetablePort

class GetTermStartMsUseCase(
    private val timetable: TimetablePort,
) {
    operator fun invoke(): Long = timetable.getTermStartMs()
}
