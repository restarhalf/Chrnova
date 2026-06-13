package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course

/**
 * 带冲突检测的调课用例
 * 
 * 封装TransCourseUseCase，提供自动获取课程列表的能力。
 */
class TransCourseWithConflictsUseCase(
    private val getAllCoursesOnce: GetAllCoursesOnceUseCase,
    private val transCourse: TransCourseUseCase,
) {
    /**
     * 调课结果
     * 
     * @param overrideCourse 调课后的课程
     * @param conflicts 冲突的课程列表
     */
    data class Result(
        val overrideCourse: Course,
        val conflicts: List<Course>,
    )

    /**
     * 执行调课操作（带预获取的课程列表）
     * 
     * @param allCourses 所有课程列表
     * @param originCourse 原始课程
     * @param originWeek 原始周次
     * @param targetWeek 目标周次
     * @param newRoom 新教室
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @return 调课结果
     */
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

    /**
     * 执行调课操作（自动获取课程列表）
     * 
     * @param originCourse 原始课程
     * @param originWeek 原始周次
     * @param targetWeek 目标周次
     * @param newRoom 新教室
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @return 调课结果
     */
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
