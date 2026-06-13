package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 设置总周数用例
 * 
 * 设置学期总周数。
 */
class SetTotalWeeksUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 设置总周数
     * 
     * @param weeks 总周数
     */
    operator fun invoke(weeks: Int) {
        timetable.setTotalWeeks(weeks)
    }
}
