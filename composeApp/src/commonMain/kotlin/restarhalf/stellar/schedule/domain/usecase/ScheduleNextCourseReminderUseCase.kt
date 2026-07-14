package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.CourseReminderPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 安排下次课程提醒用例
 *
 * 根据当前课程和校园设置，安排下一次课程的提醒。
 *
 * @param courseRepository 课程仓库
 * @param courseReminder 课程提醒端口
 */
class ScheduleNextCourseReminderUseCase(
    private val courseRepository: CourseRepository,
    private val courseReminder: CourseReminderPort,
) {
    /**
     * 安排下次课程提醒
     *
     * @param campus 校园信息
     * @param termStartMs 学期开始时间戳（毫秒）
     * @param totalWeeks 总周数
     */
    suspend operator fun invoke(
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ) {
        val courses = courseRepository.getAllCoursesOnce()
        courseReminder.scheduleNextReminder(
            courses = courses,
            campus = campus,
            termStartMs = termStartMs,
            totalWeeks = totalWeeks,
        )
    }
}
