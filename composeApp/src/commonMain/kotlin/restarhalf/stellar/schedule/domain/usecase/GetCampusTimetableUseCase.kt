package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 获取校区课表时间用例
 * 
 * 获取指定校区的课程表时间配置。
 */
class GetCampusTimetableUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 获取校区课表时间
     * 
     * @param campus 校区
     * @return 时间槽列表
     */
    operator fun invoke(campus: Campus): List<TimetableSlot> = timetable.getCampusTimetable(campus)
}
