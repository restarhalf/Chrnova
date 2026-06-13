package restarhalf.stellar.schedule.domain.port

/**
 * 提醒调度端口接口
 * 
 * 定义课程和考试提醒调度的抽象接口。
 */
interface ReminderSchedulerPort {
    /** 立即触发提醒调度，重新计算并设置所有提醒 */
    fun scheduleNow()
}
