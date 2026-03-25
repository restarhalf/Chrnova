package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.course.effectiveCoursesForWeek
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.domain.model.Course

class TransCourseUseCase {

    data class Result(
        val overrideCourse: Course,
        val conflicts: List<Course>,
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

        val overrideCourse =
            originCourse.copy(
                id = 0,
                type = 2,
                originRemoteKey = originCourse.remoteKey,
                remoteKey = originCourse.remoteKey + "#override#" + originWeek + "#" + targetWeek,
                targetWeek = targetWeek,
                location = newRoom.ifBlank { originCourse.location },
                dayOfWeek = dayOfWeek,
                startSection = startSection,
                sectionCount = (endSection - startSection + 1).coerceAtLeast(1),
                weeks = listOf(originWeek),
            )

        fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean {
            return maxOf(aStart, bStart) <= minOf(aEnd, bEnd)
        }

        val aStart = overrideCourse.startSection
        val aEnd = overrideCourse.startSection + overrideCourse.sectionCount - 1

        val effective = effectiveCoursesForWeek(all = allCourses, week = targetWeek)
        val conflicts =
            effective
                .asSequence()
                .filter { it.dayOfWeek == overrideCourse.dayOfWeek }
                .filter { isCourseActiveInWeek(it, targetWeek) }
                .filterNot { other -> other.id == originCourse.id }
                .filter { other ->
                    val bStart = other.startSection
                    val bEnd = other.startSection + other.sectionCount - 1
                    overlaps(aStart, aEnd, bStart, bEnd)
                }
                .toList()

        return Result(overrideCourse = overrideCourse, conflicts = conflicts)
    }
}


