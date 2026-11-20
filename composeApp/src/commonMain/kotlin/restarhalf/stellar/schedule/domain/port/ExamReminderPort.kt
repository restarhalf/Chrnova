package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Examination

interface ExamReminderPort {
    suspend fun scheduleNextReminder(exams: List<Examination>): ReminderScheduleResult

    suspend fun hasScheduledAlarm(): Boolean

    fun nextScheduledAtMs(): Long?

    fun cancelAll()
}
