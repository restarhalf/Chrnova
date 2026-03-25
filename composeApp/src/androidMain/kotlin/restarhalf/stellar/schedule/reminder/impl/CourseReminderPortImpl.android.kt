package restarhalf.stellar.schedule.reminder.impl

import android.Manifest
import androidx.annotation.RequiresPermission
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.reminder.CourseReminderScheduler

class CourseReminderPortImpl(
    private val scheduler: CourseReminderScheduler,
) : CourseReminderPort {
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun scheduleNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ): CourseReminderPort.ScheduleResult {
        val result =
            scheduler.scheduleNextReminder(
                courses = courses,
                campus =
                    when (campus) {
                        Campus.Development -> restarhalf.stellar.schedule.data.local.Campus.Development
                        Campus.Jinshitan -> restarhalf.stellar.schedule.data.local.Campus.Jinshitan
                    },
                termStartMs = termStartMs,
                totalWeeks = totalWeeks
            )

        return when (result) {
            is CourseReminderScheduler.ScheduleResult.Scheduled ->
                CourseReminderPort.ScheduleResult.Scheduled(result.triggerAtMs)

            is CourseReminderScheduler.ScheduleResult.NoUpcoming ->
                CourseReminderPort.ScheduleResult.NoUpcoming

            is CourseReminderScheduler.ScheduleResult.Failed ->
                CourseReminderPort.ScheduleResult.Failed
        }
    }

    override fun hasScheduledAlarm(): Boolean = scheduler.hasScheduledAlarm()

    override fun cancelAll() {
        scheduler.cancelAll()
    }
}
