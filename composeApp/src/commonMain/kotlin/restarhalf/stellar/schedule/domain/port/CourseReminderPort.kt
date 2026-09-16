package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course

/**
 * 课程提醒端口接口
 * 
 * 定义课程提醒调度的抽象接口，用于设置下一次课程提醒。
 */
interface CourseReminderPort {
    /**
     * 调度下一次课程提醒
     * 
     * @param courses 所有课程列表
     * @param campus 当前校区
     * @param termStartMs 学期开始时间戳
     * @param totalWeeks 学期总周数
     * @return 调度结果
     */
    fun scheduleNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ): ScheduleResult

    /** 检查是否已有设置的提醒 */
    fun hasScheduledAlarm(): Boolean
    /** 取消所有课程提醒 */
    fun cancelAll()

    /**
     * 提醒调度结果密封类
     */
    sealed class ScheduleResult {
        /** 成功调度，包含触发时间戳 */
        data class Scheduled(val triggerAtMs: Long) : ScheduleResult()
        /** 没有即将到来的课程 */
        data object NoUpcoming : ScheduleResult()
        /** 调度失败 */
        data object Failed : ScheduleResult()
    }
}
