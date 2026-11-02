package restarhalf.stellar.schedule.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.reminder.ExamReminderPlanner
import restarhalf.stellar.schedule.reminder.receiver.ExamReminderReceiver
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class ExamReminderScheduler(
    private val context: Context,
    private val settings: ObservableSettings,
) {

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalTime::class)
    fun scheduleNextReminder(
        exams: List<Examination>,
        nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    ): ReminderScheduleResult {
        val next =
            ExamReminderPlanner.findNext(exams = exams, nowMs = nowMs)
                ?: run {
                    cancelAll()
                    return ReminderScheduleResult.NoUpcoming
                }

        val intent =
            Intent(context, ExamReminderReceiver::class.java).apply {
                putExtra(EXTRA_COURSE_NAME, next.courseName)
                putExtra(EXTRA_PLACE, next.place)
                putExtra(EXTRA_EXAM_TIME, next.timeRaw)
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                ReminderKind.Exam.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val success =
            ReminderAlarmScheduler.schedule(
                context = context,
                triggerAtMs = next.triggerAtMs,
                pendingIntent = pendingIntent,
                logLabel = "考试提醒",
            )

        if (!success) return ReminderScheduleResult.Failed

        ReminderAlarmScheduler.writeNextTrigger(
            settings = settings,
            key = ReminderKind.Exam.triggerKey,
            triggerAtMs = next.triggerAtMs,
        )
        return ReminderScheduleResult.Scheduled(next.triggerAtMs)
    }

    fun cancelAll() {
        ReminderAlarmScheduler.cancel(
            context = context,
            requestCode = ReminderKind.Exam.requestCode,
            receiver = ExamReminderReceiver::class.java,
        )
        ReminderAlarmScheduler.clearNextTrigger(settings, ReminderKind.Exam.triggerKey)
    }

    fun hasScheduledAlarm(): Boolean {
        if (getNextTriggerTime() == null) return false
        return ReminderAlarmScheduler.hasPending(
            context = context,
            requestCode = ReminderKind.Exam.requestCode,
            receiver = ExamReminderReceiver::class.java,
        )
    }

    fun getNextTriggerTime(): Long? =
        ReminderAlarmScheduler.readNextTrigger(
            settings = settings,
            key = ReminderKind.Exam.triggerKey,
            nowMs = System.currentTimeMillis(),
        )

    companion object {
        const val EXTRA_COURSE_NAME = "exam_course_name"
        const val EXTRA_PLACE = "exam_place"
        const val EXTRA_EXAM_TIME = "exam_time"
    }
}
