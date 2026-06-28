package restarhalf.stellar.schedule.reminder.impl

import android.os.Build
import androidx.annotation.RequiresApi
import restarhalf.stellar.schedule.data.remote.JwxtExaminationItem
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.ExamReminderPort
import restarhalf.stellar.schedule.reminder.ExamReminderScheduler

class ExamReminderPortImpl(
    private val scheduler: ExamReminderScheduler,
) : ExamReminderPort {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun scheduleNextReminder(exams: List<Examination>): ExamReminderPort.ScheduleResult {
        val items = exams.map {
            JwxtExaminationItem(
                courseNumber = it.courseNumber,
                courseName = it.courseName,
                time = it.time,
                examinationPlace = it.examinationPlace,
                zwh = it.zwh,
                ksbz = it.ksbz
            )
        }
        return when (val result = scheduler.scheduleNextReminder(items)) {
            is ExamReminderScheduler.ScheduleResult.Scheduled ->
                ExamReminderPort.ScheduleResult.Scheduled(result.triggerAtMs)

            is ExamReminderScheduler.ScheduleResult.NoUpcoming ->
                ExamReminderPort.ScheduleResult.NoUpcoming

            is ExamReminderScheduler.ScheduleResult.Failed ->
                ExamReminderPort.ScheduleResult.Failed
        }
    }

    override fun hasScheduledAlarm(): Boolean = scheduler.hasScheduledAlarm()

    override fun cancelAll() {
        scheduler.cancelAll()
    }
}
