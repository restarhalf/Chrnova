package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 设置校区用例
 * 
 * 设置当前校区。
 */
class SetCampusUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 设置校区
     * 
     * @param campus 校区枚举
     */
    operator fun invoke(campus: Campus) {
        timetable.setCampus(campus)
    }
}
