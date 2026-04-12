package restarhalf.stellar.schedule.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.course.effectiveCoursesForWeek
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.data.local.Campus
import restarhalf.stellar.schedule.data.local.getCampusTimetable
import restarhalf.stellar.schedule.reminder.receiver.CourseReminderReceiver
import kotlin.time.ExperimentalTime
import restarhalf.stellar.schedule.domain.model.Course as DomainCourse

class CourseReminderScheduler(
    private val context: Context,
    private val settings: ObservableSettings
) {

    data class NextReminder(
        val triggerAtMs: Long,
        val course: DomainCourse,
        val slotStart: String,
        val slotEnd: String
    )

    @OptIn(ExperimentalTime::class)
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleNextReminder(
        courses: List<DomainCourse>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): ScheduleResult {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        getCampusTimetable(campus)

        val next =
            findNextReminder(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs
            )
                ?: run {
                    cancelAll()
                    return ScheduleResult.NoUpcoming
                }

        val intent =
            Intent(context, CourseReminderReceiver::class.java).apply {
                putExtra(EXTRA_COURSE_NAME, next.course.name)
                putExtra(EXTRA_COURSE_LOCATION, next.course.location)
                putExtra(EXTRA_COURSE_TIME, next.slotStart)
                putExtra(EXTRA_COURSE_END_TIME, next.slotEnd)
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                SINGLE_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val success =
            scheduleAlarmBestEffort(
                alarmManager = alarmManager,
                triggerAtMs = next.triggerAtMs,
                pendingIntent = pendingIntent
            )

        if (success) {
            settings[KEY_CODES] = encodeCodes(setOf(SINGLE_REMINDER_REQUEST_CODE.toString()))
            settings[KEY_NEXT_TRIGGER] = next.triggerAtMs
            return ScheduleResult.Scheduled(next.triggerAtMs)
        } else {
            return ScheduleResult.Failed
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun findNextReminder(
        courses: List<DomainCourse>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long
    ): NextReminder? {
        val timetable = getCampusTimetable(campus)
        val today = kotlin.time.Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        today.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

        var best: NextReminder? = null
        val scanDays = 14
        for (offset in 0 until scanDays) {
            val targetDay = today.plus(offset, DateTimeUnit.DAY)
            val targetDayStartMs =
                targetDay.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

            val weekInfo =
                WeekCalculator.detect(
                    totalWeeks = totalWeeks,
                    termStartMs = termStartMs,
                    nowMs = targetDayStartMs + (12 * 60 * 60 * 1000L)
                ) // 12:00
            if (weekInfo.isHoliday) continue

            val week = weekInfo.week
            val dayOfWeekMon1 = targetDay.dayOfWeek.isoDayNumber

            val effective = effectiveCoursesForWeek(all = courses, week = week)
            val dayCourses =
                effective.filter { it.dayOfWeek == dayOfWeekMon1 && isCourseActiveInWeek(it, week) }

            for (course in dayCourses) {
                val slot = timetable.getOrNull(course.startSection - 1) ?: continue
                val endSection = course.startSection + course.sectionCount - 1
                val endSlot = timetable.getOrNull(endSection - 1) ?: continue

                val (hour, minute) = ClockTime.parseToHourMinute(slot.start) ?: continue

                val triggerTime = targetDay.atStartOfDayIn(TimeZone.currentSystemDefault())
                    .toEpochMilliseconds() + (hour * 60L + minute - REMINDER_MINUTES_BEFORE) * 60 * 1000L

                if (triggerTime <= nowMs) continue

                val candidate =
                    NextReminder(
                        triggerAtMs = triggerTime,
                        course = course,
                        slotStart = slot.start,
                        slotEnd = endSlot.end
                    )

                if (best == null || candidate.triggerAtMs < best.triggerAtMs) {
                    best = candidate
                }
            }

            if (best != null) break
        }

        return best
    }

    /** 取消所有已调度的提醒 */
    fun cancelAll() {
        val codes = decodeCodes(settings.getString(KEY_CODES, ""))
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        for (codeStr in codes) {
            val code = codeStr.toIntOrNull() ?: continue
            val intent = Intent(context, CourseReminderReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    code,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        val singleIntent = Intent(context, CourseReminderReceiver::class.java)
        val singlePendingIntent =
            PendingIntent.getBroadcast(
                context,
                SINGLE_REMINDER_REQUEST_CODE,
                singleIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        if (singlePendingIntent != null) {
            alarmManager.cancel(singlePendingIntent)
            singlePendingIntent.cancel()
        }

        settings.remove(KEY_CODES)
    }

    private fun encodeCodes(codes: Set<String>): String = codes.joinToString(",")

    private fun decodeCodes(raw: String): Set<String> {
        if (raw.isBlank()) return emptySet()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    companion object {
        private const val PREFS_NAME = "reminder_codes"
        private const val KEY_CODES = "codes"
        private const val KEY_NEXT_TRIGGER = "next_trigger"
        private const val REMINDER_MINUTES_BEFORE = 16

        private const val SINGLE_REMINDER_REQUEST_CODE = 20001

        private const val EXTRA_COURSE_NAME = "course_name"
        private const val EXTRA_COURSE_LOCATION = "course_location"
        private const val EXTRA_COURSE_TIME = "course_time"
        private const val EXTRA_COURSE_END_TIME = "course_end_time"
    }

    /** 调度结果 */
    sealed class ScheduleResult {
        /** 成功调度，包含触发时间 */
        data class Scheduled(val triggerAtMs: Long) : ScheduleResult()

        /** 没有即将到来的课程 */
        object NoUpcoming : ScheduleResult()

        /** 调度失败 */
        object Failed : ScheduleResult()
    }

    /**
     * 尝试调度闹钟
     *
     * @return true 表示成功设置闹钟，false 表示失败
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private fun scheduleAlarmBestEffort(
        alarmManager: AlarmManager,
        triggerAtMs: Long,
        pendingIntent: PendingIntent
    ): Boolean {
        return try {
            val canExact =
                alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            }
            true
        } catch (e: SecurityException) {

            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
                true
            } catch (_: Exception) {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /** 检查当前是否有有效的闹钟已调度 */
    fun hasScheduledAlarm(): Boolean {
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                SINGLE_REMINDER_REQUEST_CODE,
                Intent(context, CourseReminderReceiver::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        return pendingIntent != null
    }

    /**
     * 获取下次提醒的触发时间
     *
     * @return 触发时间戳，如果没有则返回 null
     */
    fun getNextTriggerTime(): Long? {
        return if (settings.hasKey(KEY_NEXT_TRIGGER)) settings.getLong(
            KEY_NEXT_TRIGGER,
            0
        ) else null
    }
}
