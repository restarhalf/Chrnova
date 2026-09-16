package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Examination

/**
 * 考试提醒端口接口
 * 
 * 定义考试提醒调度的抽象接口，用于设置下一次考试提醒。
 */
interface ExamReminderPort {
    /**
     * 调度下一次考试提醒
     * 
     * @param exams 所有考试安排列表
     * @return 调度结果
     */
    fun scheduleNextReminder(exams: List<Examination>): ScheduleResult

    /** 检查是否已有设置的提醒 */
    fun hasScheduledAlarm(): Boolean
    /** 取消所有考试提醒 */
    fun cancelAll()

    /**
     * 提醒调度结果密封类
     */
    sealed class ScheduleResult {
        /** 成功调度，包含触发时间戳 */
        data class Scheduled(val triggerAtMs: Long) : ScheduleResult()
        /** 没有即将到来的考试 */
        data object NoUpcoming : ScheduleResult()
        /** 调度失败 */
        data object Failed : ScheduleResult()
    }
}
