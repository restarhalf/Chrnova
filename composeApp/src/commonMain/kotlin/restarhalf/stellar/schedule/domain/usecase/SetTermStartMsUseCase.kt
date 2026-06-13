package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 设置学期开始时间用例
 * 
 * 设置学期开始时间戳。
 */
class SetTermStartMsUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 设置学期开始时间
     * 
     * @param ms 时间戳（毫秒）
     */
    operator fun invoke(ms: Long) {
        timetable.setTermStartMs(ms)
    }
}
