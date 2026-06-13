package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 获取学期开始时间用例
 * 
 * 获取当前学期开始时间戳。
 */
class GetTermStartMsUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 获取学期开始时间
     * 
     * @return 时间戳（毫秒）
     */
    operator fun invoke(): Long = timetable.getTermStartMs()
}
