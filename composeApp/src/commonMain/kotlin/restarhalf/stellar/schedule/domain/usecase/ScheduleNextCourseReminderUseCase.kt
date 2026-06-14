package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.CourseReminderPort

/**
 * 安排下次课程提醒用例
 *
 * 根据当前课程和校园设置，安排下一次课程的提醒。
 *
 * @param getAllCoursesOnce 获取所有课程用例
 * @param courseReminder 课程提醒端口
 */
class ScheduleNextCourseReminderUseCase(
    private val getAllCoursesOnce: GetAllCoursesOnceUseCase,
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
        val courses = getAllCoursesOnce()
        courseReminder.scheduleNextReminder(
            courses = courses,
            campus = campus,
            termStartMs = termStartMs,
            totalWeeks = totalWeeks,
        )
    }
}
