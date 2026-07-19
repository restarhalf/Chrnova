package restarhalf.stellar.schedule.domain.usecase

import androidx.compose.runtime.Stable
import restarhalf.stellar.schedule.core.course.effectiveCoursesForWeek
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.TimetableSlot

class BuildHomeTodayScheduleUseCase {

    @Stable
    data class PeriodItem(val startSection: Int, val endSection: Int, val course: Course?)

    @Stable
    data class HomeTodaySchedule(
        val activeWeek: Int?,
        val todayCourses: List<Course>,
        val morningItems: List<PeriodItem>,
        val afternoonItems: List<PeriodItem>,
        val eveningItems: List<PeriodItem>,
        val hasFirstClass: Boolean,
    )

    operator fun invoke(
        courses: List<Course>,
        totalWeeks: Int,
        termStartMs: Long,
        todayDayOfWeekMon1: Int,
        nowMs: Long,
    ): HomeTodaySchedule {
        val weekInfo =
            WeekCalculator.detect(totalWeeks = totalWeeks, termStartMs = termStartMs, nowMs = nowMs)
        val activeWeek = if (weekInfo.isHoliday) null else weekInfo.week
        val todayCourses =
            if (activeWeek == null) {
                emptyList()
            } else {
                effectiveCoursesForWeek(all = courses, week = activeWeek)
                    .filter {
                        it.dayOfWeek == todayDayOfWeekMon1 && isCourseActiveInWeek(it, activeWeek)
                    }
            }
        return HomeTodaySchedule(
            activeWeek = activeWeek,
            todayCourses = todayCourses,
            morningItems = buildPeriodItems(
                startSection = 1,
                endSection = 4,
                periodCourses = todayCourses
            ),
            afternoonItems = buildPeriodItems(
                startSection = 5,
                endSection = 8,
                periodCourses = todayCourses
            ),
            eveningItems = buildPeriodItems(
                startSection = 9,
                endSection = 12,
                periodCourses = todayCourses
            ),
            hasFirstClass = coursesInRange(
                courses = todayCourses,
                startSection = 1,
                endSection = 1
            ).isNotEmpty(),
        )
    }

    fun timeRange(
        timetable: List<TimetableSlot>,
        startSection: Int,
        endSection: Int,
    ): Pair<String, String> {
        val start = timetable.getOrNull(startSection - 1)?.start ?: "--"
        val end = timetable.getOrNull(endSection - 1)?.end ?: "--"
        return start to end
    }

    private fun coursesInRange(
        courses: List<Course>,
        startSection: Int,
        endSection: Int,
    ): List<Course> {
        return courses.filter { course ->
            val cStart = course.startSection
            val cEnd = course.startSection + course.sectionCount - 1
            cStart <= endSection && cEnd >= startSection
        }
    }

    private fun buildPeriodItems(
        startSection: Int,
        endSection: Int,
        periodCourses: List<Course>,
    ): List<PeriodItem> {
        if (periodCourses.isEmpty()) {
            return listOf(PeriodItem(startSection, endSection, null))
        }

        val sorted = periodCourses.sortedBy { it.startSection }
        val items = ArrayList<PeriodItem>()
        var cursor = startSection

        for (course in sorted) {
            val cStart = course.startSection
            val cEnd = course.startSection + course.sectionCount - 1
            if (cEnd < startSection) continue
            if (cStart > endSection) break

            if (cStart > cursor) {
                items.add(PeriodItem(cursor, cStart - 1, null))
            }

            if (cEnd >= cursor) {
                val itemStart = maxOf(cStart, cursor)
                val itemEnd = minOf(cEnd, endSection)
                items.add(PeriodItem(itemStart, itemEnd, course))
                cursor = itemEnd + 1
            }
        }

        if (cursor <= endSection) {
            items.add(PeriodItem(cursor, endSection, null))
        }

        return items
    }
}