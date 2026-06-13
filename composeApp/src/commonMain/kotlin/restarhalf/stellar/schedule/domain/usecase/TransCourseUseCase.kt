package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.course.effectiveCoursesForWeek
import restarhalf.stellar.schedule.core.course.isCourseActiveInWeek
import restarhalf.stellar.schedule.domain.model.Course

/**
 * 调课用例
 * 
 * 处理课程调整到其他周次的逻辑，生成调课后的课程并检测冲突。
 */
class TransCourseUseCase {

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
     * 执行调课操作
     * 
     * @param allCourses 所有课程列表
     * @param originCourse 原始课程
     * @param originWeek 原始周次
     * @param targetWeek 目标周次
     * @param newRoom 新教室
     * @param dayOfWeek 星期几
     * @param startSection 开始节次
     * @param endSection 结束节次
     * @return 调课结果，包含调课后的课程和冲突列表
     */
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

        // 构建调课后的课程
        val overrideCourse =
            originCourse.copy(
                id = 0,
                type = 2,  // 调课类型
                originRemoteKey = originCourse.remoteKey,
                remoteKey = originCourse.remoteKey + "#override#" + originWeek + "#" + targetWeek,
                targetWeek = targetWeek,
                location = newRoom.ifBlank { originCourse.location },
                dayOfWeek = dayOfWeek,
                startSection = startSection,
                sectionCount = (endSection - startSection + 1).coerceAtLeast(1),
                weeks = listOf(originWeek),
            )

        /**
         * 检查两个节次范围是否重叠
         */
        fun overlaps(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean {
            return maxOf(aStart, bStart) <= minOf(aEnd, bEnd)
        }

        val aStart = overrideCourse.startSection
        val aEnd = overrideCourse.startSection + overrideCourse.sectionCount - 1

        // 检测冲突
        val effective = effectiveCoursesForWeek(all = allCourses, week = targetWeek)
        val conflicts =
            effective
                .asSequence()
                .filter { it.dayOfWeek == overrideCourse.dayOfWeek }
                .filter { isCourseActiveInWeek(it, targetWeek) }
                .filter { other ->
                    val bStart = other.startSection
                    val bEnd = other.startSection + other.sectionCount - 1
                    overlaps(aStart, aEnd, bStart, bEnd)
                }
                .toList()

        return Result(overrideCourse = overrideCourse, conflicts = conflicts)
    }
}


