package restarhalf.stellar.schedule.domain.port

sealed class ReminderScheduleResult {
    data class Scheduled(val triggerAtMs: Long) : ReminderScheduleResult()
    data object NoUpcoming : ReminderScheduleResult()
    data object Failed : ReminderScheduleResult()
}
