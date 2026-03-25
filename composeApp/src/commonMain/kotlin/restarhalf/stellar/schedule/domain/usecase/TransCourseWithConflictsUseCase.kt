package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course

class TransCourseWithConflictsUseCase(
    private val getAllCoursesOnce: GetAllCoursesOnceUseCase,
    private val transCourse: TransCourseUseCase,
) {
    data class Result(
        val overrideCourse: Course,
        val conflicts: List<Course>,
    )

    suspend operator fun invoke(
        allCourses: List<Course>,
        originCourse: Course,
        originWeek: Int,
        targetWeek: Int,
        newRoom: String,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
    ): Result {
        val result =
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
        return Result(overrideCourse = result.overrideCourse, conflicts = result.conflicts)
    }

    suspend operator fun invoke(
        originCourse: Course,
        originWeek: Int,
        targetWeek: Int,
        newRoom: String,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
    ): Result {
        return invoke(
            allCourses = getAllCoursesOnce(),
            originCourse = originCourse,
            originWeek = originWeek,
            targetWeek = targetWeek,
            newRoom = newRoom,
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            endSection = endSection,
        )
    }
}
