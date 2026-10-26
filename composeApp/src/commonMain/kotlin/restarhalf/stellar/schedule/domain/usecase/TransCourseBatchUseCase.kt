package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course

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
    ) {
        val hasConflicts: Boolean get() = weekConflicts.isNotEmpty()

        /** 有周次真正改了时间、教室或周次；全是空操作时为 false。 */
        val hasChanges: Boolean get() = overrideCourses.isNotEmpty()
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

        // 选中且原本就有的周：本周内改时间/教室。
        val sameWeeks = selected.filter { it in originWeeks }
        // 取消的原周与新增的周按顺序配对，相当于把课从原周换到新周，而不是多出一节。
        val removedWeeks = originWeeks.filterNot { it in selected }
        val addedWeeks = selected.filterNot { it in originWeeks }
        val weekMoves = removedWeeks.zip(addedWeeks)

        val overrideCourses = ArrayList<Course>(selected.size)
        val weekConflicts = ArrayList<WeekConflict>()
        val noOpWeeks = ArrayList<Int>()

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
                noOpWeeks.add(originWeek)
                return
            }
            overrideCourses.add(single.overrideCourse)

            val others = single.conflicts.filterNot { isSameCourse(it, originCourse) }
            if (others.isNotEmpty()) {
                weekConflicts.add(WeekConflict(week = targetWeek, courses = others))
            }
        }

        for (week in sameWeeks) {
            collect(originWeek = week, targetWeek = week)
        }
        for ((originWeek, targetWeek) in weekMoves) {
            collect(originWeek = originWeek, targetWeek = targetWeek)
        }

        return Result(
            overrideCourses = overrideCourses,
            weekConflicts = weekConflicts,
            noOpWeeks = noOpWeeks,
        )
    }

    private fun isSameCourse(first: Course, second: Course): Boolean {
        if (first.id != 0L || second.id != 0L) return first.id == second.id
        return first.name == second.name && first.teacher == second.teacher
    }
}
