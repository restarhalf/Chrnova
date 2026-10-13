package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Examination

interface ExamReminderPort {
    fun scheduleNextReminder(exams: List<Examination>): ScheduleResult

    fun hasScheduledAlarm(): Boolean
    fun cancelAll()

    sealed class ScheduleResult {
        data class Scheduled(val triggerAtMs: Long) : ScheduleResult()
        data object NoUpcoming : ScheduleResult()
        data object Failed : ScheduleResult()
    }
}
