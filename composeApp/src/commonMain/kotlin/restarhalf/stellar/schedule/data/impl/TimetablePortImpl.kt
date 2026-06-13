package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.data.local.TimetableSettings
import restarhalf.stellar.schedule.data.local.getCampusTimetable
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.mapper.toData
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.TimetablePort

/**
 * 课表时间表端口实现类
 * 
 * 实现TimetablePort接口，负责课表时间配置的读写操作。
 * 包括校区、学期开始时间、总周数等设置。
 * 
 * @param prefs 课表设置存储
 */
class TimetablePortImpl(
    private val prefs: TimetableSettings,
) : TimetablePort {

    /**
     * 获取当前校区
     * 
     * @return 校区枚举
     */
    override fun getCampus(): Campus = prefs.getCampus().toDomain()

    /**
     * 设置校区
     * 
     * @param campus 校区枚举
     */
    override fun setCampus(campus: Campus) {
        prefs.setCampus(campus.toData())
    }

    /**
     * 观察校区变化
     * 
     * @return 校区Flow
     */
    override fun observeCampus() = prefs.observeCampus().map { it.toDomain() }

    /**
     * 获取校区课表时间配置
     * 
     * @param campus 校区
     * @return 时间槽列表
     */
    override fun getCampusTimetable(campus: Campus): List<TimetableSlot> {
        return getCampusTimetable(campus.toData()).map { slot ->
            TimetableSlot(num = slot.num, start = slot.start, end = slot.end)
        }
    }

    /**
     * 获取学期开始时间戳
     * 
     * @return 时间戳（毫秒）
     */
    override fun getTermStartMs(): Long = prefs.getTermStartMs()

    /**
     * 设置学期开始时间戳
     * 
     * @param ms 时间戳（毫秒）
     */
    override fun setTermStartMs(ms: Long) {
        prefs.setTermStartMs(ms)
    }

    /**
     * 观察学期开始时间变化
     * 
     * @return 时间戳Flow
     */
    override fun observeTermStartMs() = prefs.observeTermStartMs()

    /**
     * 获取学期总周数
     * 
     * @return 总周数
     */
    override fun getTotalWeeks(): Int = prefs.getTotalWeeks()

    /**
     * 设置学期总周数
     * 
     * @param weeks 总周数
     */
    override fun setTotalWeeks(weeks: Int) {
        prefs.setTotalWeeks(weeks)
    }

    /**
     * 观察总周数变化
     * 
     * @return 总周数Flow
     */
    override fun observeTotalWeeks() = prefs.observeTotalWeeks()
}
