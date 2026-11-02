package restarhalf.stellar.schedule.domain.reminder

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import restarhalf.stellar.schedule.core.time.ClockTime
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.policy.effectiveCoursesForWeek
import restarhalf.stellar.schedule.domain.policy.getCampusTimetable
import restarhalf.stellar.schedule.domain.policy.isCourseActiveInWeek
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class CourseReminderPlan(
    val triggerAtMs: Long,
    val courseName: String,
    val place: String,
    val timeStart: String,
    val timeEnd: String,
)

object CourseReminderPlanner {

    private const val SCAN_DAYS = 14

    @OptIn(ExperimentalTime::class)
    fun findNext(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long,
    ): CourseReminderPlan? {
        val timeZone = TimeZone.currentSystemDefault()
        val timetable = getCampusTimetable(campus)
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date

        var best: CourseReminderPlan? = null

        for (offset in 0 until SCAN_DAYS) {
            val targetDay = today.plus(offset, DateTimeUnit.DAY)
            val targetDayStartMs = targetDay.atStartOfDayIn(timeZone).toEpochMilliseconds()

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
                effective.filter { it.dayOfWeek == dayOfWeekMon1 && isCourseActiveInWeek(it, week) }

            for (course in dayCourses) {
                val slot = timetable.getOrNull(course.startSection - 1) ?: continue
                val endSection = course.startSection + course.sectionCount - 1
                val endSlot = timetable.getOrNull(endSection - 1) ?: continue

                val (hour, minute) = ClockTime.parseToHourMinute(slot.start) ?: continue
                val triggerAtMs =
                    targetDayStartMs +
                        (hour * 60L + minute - ReminderTiming.LEAD_MINUTES) * 60 * 1000L

                if (triggerAtMs <= nowMs) continue

                val candidate =
                    CourseReminderPlan(
                        triggerAtMs = triggerAtMs,
                        courseName = course.name,
                        place = course.location,
                        timeStart = slot.start,
                        timeEnd = endSlot.end,
                    )

                if (best == null || candidate.triggerAtMs < best.triggerAtMs) {
                    best = candidate
                }
            }

            if (best != null) break
        }

        return best
    }
}
