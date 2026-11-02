package restarhalf.stellar.schedule.reminder.impl

import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.reminder.CourseReminderPlanner
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CourseReminderPortImpl(
    settings: ObservableSettings,
) : CourseReminderPort {

    private val state =
        IosReminderState(
            settings = settings,
            identifier = COURSE_REMINDER_ID,
            scheduledKey = KEY_COURSE_SCHEDULED,
            triggerKey = KEY_COURSE_TRIGGER,
        )

    @OptIn(ExperimentalTime::class)
    override suspend fun scheduleNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ): ReminderScheduleResult {
        val next =
            CourseReminderPlanner.findNext(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = Clock.System.now().toEpochMilliseconds(),
            )
                ?: run {
                    cancelAll()
                    return ReminderScheduleResult.NoUpcoming
                }

        return state.schedule(
            title = "课程提醒",
            body = "${next.courseName}（${next.timeStart}-${next.timeEnd}） ${next.place}",
            triggerAtMs = next.triggerAtMs,
        )
    }

    override suspend fun hasScheduledAlarm(): Boolean = state.hasScheduled()

    override fun nextScheduledAtMs(): Long? = state.nextScheduledAtMs()

    override fun cancelAll() {
        state.cancelAll()
    }

    private companion object {
        private const val COURSE_REMINDER_ID = "course.next.reminder"
        private const val KEY_COURSE_SCHEDULED = "ios_course_reminder_scheduled"
        private const val KEY_COURSE_TRIGGER = "ios_course_reminder_trigger_ms"
    }
}
