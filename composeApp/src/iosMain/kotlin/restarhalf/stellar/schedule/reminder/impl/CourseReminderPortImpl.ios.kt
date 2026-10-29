package restarhalf.stellar.schedule.reminder.impl

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.domain.policy.effectiveCoursesForWeek
import restarhalf.stellar.schedule.domain.policy.isCourseActiveInWeek
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.policy.getCampusTimetable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class CourseReminderPortImpl(
    private val settings: ObservableSettings,
) : CourseReminderPort {

    @OptIn(ExperimentalTime::class)
    override suspend fun scheduleNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ): CourseReminderPort.ScheduleResult {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val next =
            findNextReminder(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs,
            ) ?: run {
                cancelAll()
                return CourseReminderPort.ScheduleResult.NoUpcoming
            }

        val success = IosReminderNotifications.schedule(
            identifier = COURSE_REMINDER_ID,
            title = "课程提醒",
            body = "${next.course.name}（${next.slotStart}-${next.slotEnd}） ${next.course.location}",
            triggerAtMs = next.triggerAtMs,
        )

        return if (success) {
            settings[KEY_COURSE_SCHEDULED] = true
            settings[KEY_COURSE_TRIGGER] = next.triggerAtMs
            CourseReminderPort.ScheduleResult.Scheduled(next.triggerAtMs)
        } else {
            clearScheduledState()
            CourseReminderPort.ScheduleResult.Failed
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun hasScheduledAlarm(): Boolean {
        if (!settings.getBoolean(KEY_COURSE_SCHEDULED, false)) return false
        val triggerAtMs = settings.getLong(KEY_COURSE_TRIGGER, 0L)
        val nowMs = Clock.System.now().toEpochMilliseconds()
        if (triggerAtMs <= 0L || triggerAtMs <= nowMs) {
            cancelAll()
            return false
        }
        if (IosReminderNotifications.isPending(COURSE_REMINDER_ID) == false) {
            clearScheduledState()
            return false
        }
        return true
    }

    @OptIn(ExperimentalTime::class)
    override fun nextScheduledAtMs(): Long? {
        if (!settings.getBoolean(KEY_COURSE_SCHEDULED, false)) return null
        val triggerAtMs = settings.getLong(KEY_COURSE_TRIGGER, 0L)
        val nowMs = Clock.System.now().toEpochMilliseconds()
        // 通知响过之后预存值会留在原地，过期就当没挂
        if (triggerAtMs <= 0L || triggerAtMs <= nowMs) return null
        return triggerAtMs
    }

    override fun cancelAll() {
        IosReminderNotifications.cancel(COURSE_REMINDER_ID)
        clearScheduledState()
    }

    private fun clearScheduledState() {
        settings[KEY_COURSE_SCHEDULED] = false
        settings.remove(KEY_COURSE_TRIGGER)
    }

    @OptIn(ExperimentalTime::class)
    private fun findNextReminder(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long,
    ): NextReminder? {
        val timetable = getCampusTimetable(campus)
        val today = Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

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
                    nowMs = targetDayStartMs + 12 * 60 * 60 * 1000L,
                )
            if (weekInfo.isHoliday) continue

            val week = weekInfo.week
            val dayOfWeekMon1 = targetDay.dayOfWeek.isoDayNumber

            val effective = effectiveCoursesForWeek(all = courses, week = week)
            val dayCourses =
                effective.filter {
                    it.dayOfWeek == dayOfWeekMon1 && isCourseActiveInWeek(it, week)
                }

            for (course in dayCourses) {
                val slot = timetable.getOrNull(course.startSection - 1) ?: continue
                val endSection = course.startSection + course.sectionCount - 1
                val endSlot = timetable.getOrNull(endSection - 1) ?: continue

                val (hour, minute) = ClockTime.parseToHourMinute(slot.start) ?: continue
                val triggerAtMs =
                    targetDay.atStartOfDayIn(TimeZone.currentSystemDefault())
                        .toEpochMilliseconds() +
                            (hour * 60L + minute - REMINDER_MINUTES_BEFORE) * 60 * 1000L

                if (triggerAtMs <= nowMs) continue

                val candidate =
                    NextReminder(
                        triggerAtMs = triggerAtMs,
                        course = course,
                        slotStart = slot.start,
                        slotEnd = endSlot.end,
                    )
                if (best == null || candidate.triggerAtMs < best.triggerAtMs) {
                    best = candidate
                }
            }

            if (best != null) break
        }

        return best
    }

    private data class NextReminder(
        val triggerAtMs: Long,
        val course: Course,
        val slotStart: String,
        val slotEnd: String,
    )

    private companion object {
        private const val REMINDER_MINUTES_BEFORE = 16
        private const val COURSE_REMINDER_ID = "course.next.reminder"
        private const val KEY_COURSE_SCHEDULED = "ios_course_reminder_scheduled"
        private const val KEY_COURSE_TRIGGER = "ios_course_reminder_trigger_ms"
    }
}
