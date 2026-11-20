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

data class DayCoursesSummary(
    val count: Int,
    val firstCourseName: String,
    val firstStart: String,
    val firstPlace: String,
)

/** 某一天的课程及其绝对起止时刻，调度闹钟与勿扰共用。 */
data class CourseSlot(
    val courseId: Long,
    val name: String,
    val place: String,
    val startAtMs: Long,
    val endAtMs: Long,
    val timeStart: String,
    val timeEnd: String,
)

object CourseReminderPlanner {

    private const val SCAN_DAYS = 14

    /**
     * 指定偏移日（0=今天）的全部课程，按上课时刻排序。
     *
     * 跨零点的课（如 23:00-00:30）下课时刻顺延到次日，避免时长算成负数。
     */
    @OptIn(ExperimentalTime::class)
    fun planDay(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long,
        dayOffset: Int,
    ): List<CourseSlot> {
        val timeZone = TimeZone.currentSystemDefault()
        val timetable = getCampusTimetable(campus)
        val targetDay =
            Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date
                .plus(dayOffset, DateTimeUnit.DAY)
        val targetDayStartMs = targetDay.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val weekInfo =
            WeekCalculator.detect(
                totalWeeks = totalWeeks,
                termStartMs = termStartMs,
                nowMs = targetDayStartMs + 12 * 60 * 60 * 1000L,
            )
        if (weekInfo.isHoliday) return emptyList()
        val week = weekInfo.week
        val dayOfWeekMon1 = targetDay.dayOfWeek.isoDayNumber
        val dayCourses =
            effectiveCoursesForWeek(all = courses, week = week)
                .filter { it.dayOfWeek == dayOfWeekMon1 && isCourseActiveInWeek(it, week) }

        return dayCourses
            .mapNotNull { course ->
                val startSlot = timetable.getOrNull(course.startSection - 1) ?: return@mapNotNull null
                val endSlot =
                    timetable.getOrNull(course.startSection + course.sectionCount - 2)
                        ?: return@mapNotNull null
                val (startHour, startMinute) =
                    ClockTime.parseToHourMinute(startSlot.start) ?: return@mapNotNull null
                val (endHour, endMinute) =
                    ClockTime.parseToHourMinute(endSlot.end) ?: return@mapNotNull null
                val startMinutes = startHour * 60 + startMinute
                val endMinutes = endHour * 60 + endMinute
                val startAtMs = targetDayStartMs + startMinutes * 60_000L
                val rawEndAtMs = targetDayStartMs + endMinutes * 60_000L
                val endAtMs =
                    if (endMinutes <= startMinutes) {
                        rawEndAtMs + 24 * 60 * 60 * 1000L
                    } else {
                        rawEndAtMs
                    }
                CourseSlot(
                    courseId = course.id,
                    name = course.name,
                    place = course.location,
                    startAtMs = startAtMs,
                    endAtMs = endAtMs,
                    timeStart = startSlot.start,
                    timeEnd = endSlot.end,
                )
            }
            .sortedBy { it.startAtMs }
    }

    /** 指定偏移日的课程摘要（次日提醒用）：当天没课返回 null。 */
    @OptIn(ExperimentalTime::class)
    fun summarizeDay(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long,
        dayOffset: Int,
    ): DayCoursesSummary? {
        val slots =
            planDay(
                courses = courses,
                campus = campus,
                termStartMs = termStartMs,
                totalWeeks = totalWeeks,
                nowMs = nowMs,
                dayOffset = dayOffset,
            )
        val first = slots.firstOrNull() ?: return null
        return DayCoursesSummary(
            count = slots.size,
            firstCourseName = first.name,
            firstStart = first.timeStart,
            firstPlace = first.place,
        )
    }

    @OptIn(ExperimentalTime::class)
    fun findNext(
        courses: List<Course>,
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
        nowMs: Long,
        leadMinutes: Int = ReminderTiming.LEAD_MINUTES,
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
                        (hour * 60L + minute - leadMinutes) * 60 * 1000L

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
