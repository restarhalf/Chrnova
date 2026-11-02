package restarhalf.stellar.schedule.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.reminder.CourseReminderPlanner
import restarhalf.stellar.schedule.reminder.receiver.CourseReminderReceiver
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CourseReminderScheduler(
    private val context: Context,
    private val settings: ObservableSettings,
) {

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalTime::class)
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    ): ReminderScheduleResult {
        val next =
            CourseReminderPlanner.findNext(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs,
            )
                ?: run {
                    cancelAll()
                    return ReminderScheduleResult.NoUpcoming
                }

        val intent =
            Intent(context, CourseReminderReceiver::class.java).apply {
                putExtra(EXTRA_COURSE_NAME, next.courseName)
                putExtra(EXTRA_COURSE_LOCATION, next.place)
                putExtra(EXTRA_COURSE_TIME, next.timeStart)
                putExtra(EXTRA_COURSE_END_TIME, next.timeEnd)
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                ReminderKind.Course.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val success =
            ReminderAlarmScheduler.schedule(
                context = context,
                triggerAtMs = next.triggerAtMs,
                pendingIntent = pendingIntent,
                logLabel = "课程提醒",
            )

        if (!success) return ReminderScheduleResult.Failed

        ReminderAlarmScheduler.writeNextTrigger(
            settings = settings,
            key = ReminderKind.Course.triggerKey,
            triggerAtMs = next.triggerAtMs,
        )
        return ReminderScheduleResult.Scheduled(next.triggerAtMs)
    }

    fun cancelAll() {
        ReminderAlarmScheduler.cancel(
            context = context,
            requestCode = ReminderKind.Course.requestCode,
            receiver = CourseReminderReceiver::class.java,
        )
        ReminderAlarmScheduler.clearNextTrigger(settings, ReminderKind.Course.triggerKey)
    }

    fun hasScheduledAlarm(): Boolean {
        if (getNextTriggerTime() == null) return false
        return ReminderAlarmScheduler.hasPending(
            context = context,
            requestCode = ReminderKind.Course.requestCode,
            receiver = CourseReminderReceiver::class.java,
        )
    }

    fun getNextTriggerTime(): Long? =
        ReminderAlarmScheduler.readNextTrigger(
            settings = settings,
            key = ReminderKind.Course.triggerKey,
            nowMs = System.currentTimeMillis(),
        )

    companion object {
        private const val EXTRA_COURSE_NAME = "course_name"
        private const val EXTRA_COURSE_LOCATION = "course_location"
        private const val EXTRA_COURSE_TIME = "course_time"
        private const val EXTRA_COURSE_END_TIME = "course_end_time"
    }
}
