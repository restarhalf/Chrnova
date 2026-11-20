package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.policy.familyKey
import restarhalf.stellar.schedule.domain.policy.isTransOverrideOf

class TransCourseBatchUseCase(
    private val transCourse: TransCourseUseCase,
) {

    data class WeekConflict(
        val week: Int,
        val courses: List<Course>,
    )

    data class Result(
        val overrideCourses: List<Course>,
        val weekConflicts: List<WeekConflict>,
        val noOpWeeks: List<Int> = emptyList(),
        val clearedWeeks: List<Int> = emptyList(),
    ) {
        val hasConflicts: Boolean get() = weekConflicts.isNotEmpty()

        val hasChanges: Boolean get() = overrideCourses.isNotEmpty() || clearedWeeks.isNotEmpty()
    }

    operator fun invoke(
        allCourses: List<Course>,
        originCourse: Course,
        weeks: Collection<Int>,
        newRoom: String,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
    ): Result {
        val selected = weeks.filter { it > 0 }.distinct().sorted()
        val originWeeks = originCourse.weeks.filter { it > 0 }.distinct().sorted()

        val sameWeeks = selected.filter { it in originWeeks }
        val removedWeeks = originWeeks.filterNot { it in selected }
        val addedWeeks = selected.filterNot { it in originWeeks }
        val weekMoves = removedWeeks.zip(addedWeeks)

        val overrideCourses = ArrayList<Course>(selected.size)
        val weekConflicts = ArrayList<WeekConflict>()
        val noOpWeeks = ArrayList<Int>()
        val clearedWeeks = ArrayList<Int>()

        val familyKey = originCourse.familyKey()
        val overriddenWeeks =
            allCourses
                .filter { it.isTransOverrideOf(familyKey) }
                .flatMap { it.weeks }
                .toSet()

        fun collect(originWeek: Int, targetWeek: Int) {
            val single =
                transCourse(
                    allCourses = allCourses,
                    originCourse = originCourse,
                    originWeek = originWeek,
                    targetWeek = targetWeek,
                    newRoom = newRoom,
                    dayOfWeek = dayOfWeek,
                    startSection = startSection,
                    endSection = endSection,
                )
            if (single.isNoOp) {
                if (originWeek in overriddenWeeks) {
                    clearedWeeks.add(originWeek)
                } else {
                    noOpWeeks.add(originWeek)
                }
                return
            }
            overrideCourses.add(single.overrideCourse)

            if (single.conflicts.isNotEmpty()) {
                weekConflicts.add(WeekConflict(week = targetWeek, courses = single.conflicts))
            }
        }

        for (week in sameWeeks) {
            collect(originWeek = week, targetWeek = week)
        }
        for ((originWeek, targetWeek) in weekMoves) {
            collect(originWeek = originWeek, targetWeek = targetWeek)
        }
        removedWeeks.drop(weekMoves.size)
            .filter { it in overriddenWeeks }
            .forEach { clearedWeeks.add(it) }

        return Result(
            overrideCourses = overrideCourses,
            weekConflicts = weekConflicts,
            noOpWeeks = noOpWeeks,
            clearedWeeks = clearedWeeks.distinct().sorted(),
        )
    }
}
