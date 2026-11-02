package restarhalf.stellar.schedule.reminder.impl

import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.reminder.ExamReminderPlanner
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ExamReminderPortImpl(
    settings: ObservableSettings,
) : ExamReminderPort {

    private val state =
        IosReminderState(
            settings = settings,
            identifier = EXAM_REMINDER_ID,
            scheduledKey = KEY_EXAM_SCHEDULED,
            triggerKey = KEY_EXAM_TRIGGER,
        )

    @OptIn(ExperimentalTime::class)
    override suspend fun scheduleNextReminder(exams: List<Examination>): ReminderScheduleResult {
        val next =
            ExamReminderPlanner.findNext(
                exams = exams,
                nowMs = Clock.System.now().toEpochMilliseconds(),
            )
                ?: run {
                    cancelAll()
                    return ReminderScheduleResult.NoUpcoming
                }

        return state.schedule(
            title = "考试提醒",
            body = "${next.courseName} ${next.place}",
            triggerAtMs = next.triggerAtMs,
        )
    }

    override suspend fun hasScheduledAlarm(): Boolean = state.hasScheduled()

    override fun nextScheduledAtMs(): Long? = state.nextScheduledAtMs()

    override fun cancelAll() {
        state.cancelAll()
    }

    private companion object {
        private const val EXAM_REMINDER_ID = "exam.next.reminder"
        private const val KEY_EXAM_SCHEDULED = "ios_exam_reminder_scheduled"
        private const val KEY_EXAM_TRIGGER = "ios_exam_reminder_trigger_ms"
    }
}
