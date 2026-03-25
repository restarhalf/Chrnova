package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course

interface CourseReminderPort {
    fun scheduleNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ): ScheduleResult

    fun hasScheduledAlarm(): Boolean
    fun cancelAll()

    sealed class ScheduleResult {
        data class Scheduled(val triggerAtMs: Long) : ScheduleResult()
        data object NoUpcoming : ScheduleResult()
        data object Failed : ScheduleResult()
    }
}
