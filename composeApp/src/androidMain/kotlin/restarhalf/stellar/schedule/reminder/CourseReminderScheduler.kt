package restarhalf.stellar.schedule.reminder

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.reminder.CourseReminderPlanner
import restarhalf.stellar.schedule.reminder.receiver.CourseReminderReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 课程提醒调度，骨架照 Nexio 的 startReminderService：
 * 每次都是「取消登记的旧闹钟 → 按当前课表整批重排」，幂等，闹钟丢失或课表变更都能自愈。
 */
class CourseReminderScheduler(
    private val context: Context,
    /** 已排闹钟的触发时间簿记（reminder_codes）。 */
    private val settings: ObservableSettings,
    /** 用户配置（schedule_settings）。 */
    private val reminderPrefs: ObservableSettings,
) {

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalTime::class)
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    suspend fun scheduleNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    ): ReminderScheduleResult {
        CourseDayAlarmScheduler.refreshNextDay(context, reminderPrefs, nowMs)

        if (!reminderPrefs.getBoolean(SettingsKeys.REMINDER_ENABLED, false)) {
            cancelAll()
            return ReminderScheduleResult.NoUpcoming
        }

        // 排今天与明天两天的课：多排一天保证跨日时课前/勿扰闹钟已经就位，
        // 分钟级对账链（ReminderTickScheduler）再兜住丢失与延迟。
        val todaySlots =
            CourseReminderPlanner.planDay(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs,
                dayOffset = 0,
            )
        val tomorrowSlots =
            CourseReminderPlanner.planDay(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs,
                dayOffset = 1,
            )

        val schedule =
            CourseDayAlarmScheduler.scheduleDays(
                context = context,
                bookkeeping = settings,
                prefs = reminderPrefs,
                daySlots = listOf(0 to todaySlots, 1 to tomorrowSlots),
                nowMs = nowMs,
            )

        val nextAtMs = schedule.nextTriggerAtMs
        val nextRc = schedule.nextTriggerRc
        if (nextAtMs != null && nextRc != null) {
            ReminderAlarmScheduler.writeNextTrigger(
                settings = settings,
                key = KEY_NEXT_TRIGGER,
                triggerAtMs = nextAtMs,
            )
            settings[KEY_NEXT_RC] = nextRc
        } else {
            ReminderAlarmScheduler.clearNextTrigger(settings, KEY_NEXT_TRIGGER)
            settings.remove(KEY_NEXT_RC)
        }
        settings[KEY_SCHEDULED_DATE] = todayDate()

        // 开关切换 / 重排后立即对账，不必等边界闹钟
        ClassDndController.applyCurrentState(context, nowMs)
        // 分钟级对账链：跨日重排 + 课前兜底补发 + 勿扰对账，照 Nexio 的 scheduleWidgetRefresh
        ReminderTickScheduler.scheduleNext(context, nowMs)

        return nextAtMs?.let { ReminderScheduleResult.Scheduled(it) }
            ?: ReminderScheduleResult.NoUpcoming
    }

    suspend fun refreshNextDayReminder() {
        CourseDayAlarmScheduler.refreshNextDay(context, reminderPrefs)
    }

    fun cancelAll() {
        CourseDayAlarmScheduler.cancelCourseAlarms(context, settings)
        ReminderAlarmScheduler.clearNextTrigger(settings, KEY_NEXT_TRIGGER)
        settings.remove(KEY_NEXT_RC)
        settings.remove(KEY_SCHEDULED_DATE)
        ReminderTickScheduler.cancel(context)
        LiveNotificationController.cancelAll(context)
        IslandNotificationHelper.cancelAll(context)
        ClassDndController.handBackFromUser(context)
    }

    /** 对账链用：今天的闹钟是否已经按当前课表排过。 */
    fun isScheduledForToday(): Boolean =
        settings.getString(KEY_SCHEDULED_DATE, "") == todayDate()

    fun hasScheduledAlarm(): Boolean {
        if (getNextTriggerTime() == null) return false
        if (settings.getString(KEY_SCHEDULED_DATE, "") != todayDate()) return false
        val rc = settings.getInt(KEY_NEXT_RC, -1)
        if (rc < 0) return false
        return ReminderAlarmScheduler.hasPending(
            context = context,
            requestCode = rc,
            receiver = CourseReminderReceiver::class.java,
        )
    }

    fun getNextTriggerTime(): Long? =
        ReminderAlarmScheduler.readNextTrigger(
            settings = settings,
            key = KEY_NEXT_TRIGGER,
            nowMs = System.currentTimeMillis(),
        )

    private fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private companion object {
        const val KEY_NEXT_TRIGGER = "course_next_trigger"
        const val KEY_NEXT_RC = "course_next_trigger_rc"
        const val KEY_SCHEDULED_DATE = "course_scheduled_date"
    }
}
