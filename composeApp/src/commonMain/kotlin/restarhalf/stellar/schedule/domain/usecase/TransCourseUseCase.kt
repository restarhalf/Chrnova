package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.policy.belongsToFamily
import restarhalf.stellar.schedule.domain.policy.effectiveCoursesForWeek
import restarhalf.stellar.schedule.domain.policy.familyKey
import restarhalf.stellar.schedule.domain.policy.isCourseActiveInWeek
import restarhalf.stellar.schedule.domain.model.Course

class TransCourseUseCase {

    data class Result(
        val overrideCourse: Course,
        val conflicts: List<Course>,
        val isNoOp: Boolean = false,
    )

    operator fun invoke(
        allCourses: List<Course>,
        originCourse: Course,
        originWeek: Int,
        targetWeek: Int,
        newRoom: String,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
    ): Result {

        val sectionCount = (endSection - startSection + 1).coerceAtLeast(1)
        val location = newRoom.ifBlank { originCourse.location }

        val overrideCourse =
            originCourse.copy(
                id = 0,
                type = 2,
                originRemoteKey = originCourse.remoteKey,
                remoteKey = originCourse.remoteKey + "#override#" + originWeek + "#" + targetWeek,
                targetWeek = targetWeek,
                location = location,
                dayOfWeek = dayOfWeek,
                startSection = startSection,
                sectionCount = sectionCount,
                weeks = listOf(originWeek),
            )

        // 只有“原本就有这一周、且时间地点都没变”才算空操作；换到新周次是有效的换周。
        val isNoOp =
            originWeek == targetWeek &&
                originCourse.weeks.contains(originWeek) &&
                location == originCourse.location &&
                dayOfWeek == originCourse.dayOfWeek &&
                startSection == originCourse.startSection &&
                sectionCount == originCourse.sectionCount

        fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean {
            return maxOf(aStart, bStart) <= minOf(aEnd, bEnd)
        }

        val aStart = overrideCourse.startSection
        val aEnd = overrideCourse.startSection + overrideCourse.sectionCount - 1

        val familyKey = originCourse.familyKey()

        val effective = effectiveCoursesForWeek(all = allCourses, week = targetWeek)
        val conflicts =
            effective
                .asSequence()
                .filter { it.dayOfWeek == overrideCourse.dayOfWeek }
                .filter { isCourseActiveInWeek(it, targetWeek) }
                // 自己、以及这门课之前的调课记录都不算冲突。
                .filterNot { it.id != 0L && it.id == originCourse.id }
                .filterNot { it.belongsToFamily(familyKey) }
                .filter { other ->
                    val bStart = other.startSection
                    val bEnd = other.startSection + other.sectionCount - 1
                    overlaps(aStart, aEnd, bStart, bEnd)
                }
                .toList()

        return Result(overrideCourse = overrideCourse, conflicts = conflicts, isNoOp = isNoOp)
    }
}
