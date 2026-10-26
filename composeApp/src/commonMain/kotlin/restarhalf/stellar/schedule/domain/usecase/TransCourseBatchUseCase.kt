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
        /** 这些周要回到原课表，保存时清掉它们已有的调课记录。 */
        val clearedWeeks: List<Int> = emptyList(),
    ) {
        val hasConflicts: Boolean get() = weekConflicts.isNotEmpty()

        /** 有周次真正改了时间、教室、周次，或需要撤掉旧调课；全无变化时为 false。 */
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

        // 选中且原本就有的周：本周内改时间/教室。
        val sameWeeks = selected.filter { it in originWeeks }
        // 取消的原周与新增的周按顺序配对，相当于把课从原周换到新周，而不是多出一节。
        val removedWeeks = originWeeks.filterNot { it in selected }
        val addedWeeks = selected.filterNot { it in originWeeks }
        val weekMoves = removedWeeks.zip(addedWeeks)

        val overrideCourses = ArrayList<Course>(selected.size)
        val weekConflicts = ArrayList<WeekConflict>()
        val noOpWeeks = ArrayList<Int>()
        val clearedWeeks = ArrayList<Int>()

        // 这门课已有的调课记录覆盖了哪些原周，决定哪些“没改”的周其实是在撤销。
        val overriddenWeeks =
            allCourses
                .filter { it.type == 2 && it.originRemoteKey == originCourse.remoteKey }
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
                // 本来就被调走过、现在调回原样，等于撤销这一周的调课。
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
        // 取消、也没换成新周的周次：回到原课表。
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
