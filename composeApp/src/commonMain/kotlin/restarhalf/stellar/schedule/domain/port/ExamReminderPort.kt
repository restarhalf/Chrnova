package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Examination

interface ExamReminderPort {
    fun scheduleNextReminder(exams: List<Examination>): ScheduleResult

    fun hasScheduledAlarm(): Boolean

    /** 已挂的考试提醒触发时间；没挂或已过期返回 null。 */
    fun nextScheduledAtMs(): Long?

    fun cancelAll()

    sealed class ScheduleResult {
        data class Scheduled(val triggerAtMs: Long) : ScheduleResult()
        data object NoUpcoming : ScheduleResult()
        data object Failed : ScheduleResult()
    }
}
