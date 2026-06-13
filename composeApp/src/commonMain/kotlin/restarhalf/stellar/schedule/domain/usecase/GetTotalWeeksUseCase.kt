package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 获取总周数用例
 * 
 * 获取当前学期总周数。
 */
class GetTotalWeeksUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 获取总周数
     * 
     * @return 总周数
     */
    operator fun invoke(): Int = timetable.getTotalWeeks()
}
