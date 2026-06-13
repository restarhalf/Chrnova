package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 获取校区用例
 * 
 * 获取当前设置的校区。
 */
class GetCampusUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 获取当前校区
     * 
     * @return 校区枚举
     */
    operator fun invoke(): Campus = timetable.getCampus()
}
