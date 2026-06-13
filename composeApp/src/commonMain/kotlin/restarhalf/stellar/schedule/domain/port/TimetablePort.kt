package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import kotlinx.coroutines.flow.Flow

/**
 * 课表时间表端口接口
 * 
 * 定义课程表时间配置的抽象接口，包括校区、学期开始时间、总周数等。
 */
interface TimetablePort {
    /** 获取当前校区 */
    fun getCampus(): Campus
    /** 设置校区 */
    fun setCampus(campus: Campus)
    /** 观察校区变化 */
    fun observeCampus(): Flow<Campus>

    /**
     * 获取指定校区的课程表时间槽配置
     * 
     * @param campus 校区，默认为当前校区
     * @return 时间槽列表，每节课的开始和结束时间
     */
    fun getCampusTimetable(campus: Campus = getCampus()): List<TimetableSlot>

    /** 获取学期开始时间戳（毫秒） */
    fun getTermStartMs(): Long
    /** 设置学期开始时间戳（毫秒） */
    fun setTermStartMs(ms: Long)
    /** 观察学期开始时间变化 */
    fun observeTermStartMs(): Flow<Long>

    /** 获取学期总周数 */
    fun getTotalWeeks(): Int
    /** 设置学期总周数 */
    fun setTotalWeeks(weeks: Int)
    /** 观察总周数变化 */
    fun observeTotalWeeks(): Flow<Int>
}
