package restarhalf.stellar.schedule.reminder

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import restarhalf.stellar.schedule.domain.reminder.ExamReminderPlanner
import restarhalf.stellar.schedule.reminder.receiver.ExamReminderReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 考试提醒调度，与 [CourseReminderScheduler] 完全一致：
 * 每次都是「取消登记的旧闹钟 → 按当前考试安排整批重排」，幂等，闹钟丢失或考试变更都能自愈。
 */
class ExamReminderScheduler(
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
        exams: List<Examination>,
        nowMs: Long = Clock.System.now().toEpochMilliseconds(),
    ): ReminderScheduleResult {
        if (!reminderPrefs.getBoolean(SettingsKeys.REMINDER_ENABLED, false)) {
            cancelAll()
            return ReminderScheduleResult.NoUpcoming
        }

        // 排今天与明天两天的考试：多排一天保证跨日时考前/勿扰闹钟已经就位，
        // 分钟级对账链（ReminderTickScheduler）再兜住丢失与延迟。
        val todaySlots =
            ExamReminderPlanner.planDay(exams = exams, nowMs = nowMs, dayOffset = 0)
        val tomorrowSlots =
            ExamReminderPlanner.planDay(exams = exams, nowMs = nowMs, dayOffset = 1)

        val schedule =
            ExamDayAlarmScheduler.scheduleDays(
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
        ExamDndController.applyCurrentState(context, nowMs)
        // 分钟级对账链：跨日重排 + 考前兜底补发 + 勿扰对账
        ReminderTickScheduler.scheduleNext(context, nowMs)

        return nextAtMs?.let { ReminderScheduleResult.Scheduled(it) }
            ?: ReminderScheduleResult.NoUpcoming
    }

    fun cancelAll() {
        ExamDayAlarmScheduler.cancelExamAlarms(context, settings)
        ReminderAlarmScheduler.clearNextTrigger(settings, KEY_NEXT_TRIGGER)
        settings.remove(KEY_NEXT_RC)
        settings.remove(KEY_SCHEDULED_DATE)
        LiveNotificationController.cancelSession(context, ReminderSession.Exam)
        IslandNotificationHelper.cancelSession(context, ReminderSession.Exam)
        ExamDndController.handBackFromUser(context)
    }

    /** 对账链用：今天的闹钟是否已经按当前考试安排排过。 */
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
            receiver = ExamReminderReceiver::class.java,
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
        const val KEY_NEXT_TRIGGER = "exam_next_trigger"
        const val KEY_NEXT_RC = "exam_next_trigger_rc"
        const val KEY_SCHEDULED_DATE = "exam_scheduled_date"
    }
}