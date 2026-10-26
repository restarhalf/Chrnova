package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class TransCourseWithConflictsUseCase(
    private val courseRepository: CourseRepository,
    private val transCourse: TransCourseUseCase,
    private val transCourseBatch: TransCourseBatchUseCase = TransCourseBatchUseCase(transCourse),
) {
    
    data class Result(
        val overrideCourse: Course,
        val conflicts: List<Course>,
        val isNoOp: Boolean = false,
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
        return Result(
            overrideCourse = result.overrideCourse,
            conflicts = result.conflicts,
            isNoOp = result.isNoOp,
        )
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
            allCourses = courseRepository.getAllCoursesOnce(),
            originCourse = originCourse,
            originWeek = originWeek,
            targetWeek = targetWeek,
            newRoom = newRoom,
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            endSection = endSection,
        )
    }

    suspend fun buildBatch(
        originCourse: Course,
        weeks: Collection<Int>,
        newRoom: String,
        dayOfWeek: Int,
        startSection: Int,
        endSection: Int,
    ): TransCourseBatchUseCase.Result {
        return transCourseBatch(
            allCourses = courseRepository.getAllCoursesOnce(),
            originCourse = originCourse,
            weeks = weeks,
            newRoom = newRoom,
            dayOfWeek = dayOfWeek,
            startSection = startSection,
            endSection = endSection,
        )
    }
}
