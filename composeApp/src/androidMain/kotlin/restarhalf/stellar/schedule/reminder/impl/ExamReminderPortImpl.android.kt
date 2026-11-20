package restarhalf.stellar.schedule.reminder.impl

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.reminder.ExamReminderScheduler

class ExamReminderPortImpl(
    private val scheduler: ExamReminderScheduler,
) : ExamReminderPort {
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override suspend fun scheduleNextReminder(exams: List<Examination>): ReminderScheduleResult =
        scheduler.scheduleNextReminder(exams)

    override suspend fun hasScheduledAlarm(): Boolean = scheduler.hasScheduledAlarm()

    override fun nextScheduledAtMs(): Long? = scheduler.getNextTriggerTime()

    override fun cancelAll() {
        scheduler.cancelAll()
    }
}