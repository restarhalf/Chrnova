package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.TimetablePort

class SetTermStartMsUseCase(
    private val timetable: TimetablePort,
) {
    operator fun invoke(ms: Long) {
        timetable.setTermStartMs(ms)
    }
}
