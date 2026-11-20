package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course

interface CourseReminderPort {
    suspend fun scheduleNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ): ReminderScheduleResult

    suspend fun hasScheduledAlarm(): Boolean

    fun nextScheduledAtMs(): Long?

    fun cancelAll()

    /** 重排与课程无关的次日课程提醒（每天固定时刻）。 */
    suspend fun refreshNextDayReminder()
}
