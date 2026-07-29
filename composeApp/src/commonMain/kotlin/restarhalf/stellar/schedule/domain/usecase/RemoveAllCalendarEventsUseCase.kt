package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.CalendarEventPort

/**
 * 删除本应用写入的所有日历事件用例
 *
 * 关闭提醒开关时调用,清理本应用在系统日历中写入的事件。
 *
 * @param removeCourses 是否删除课程事件
 * @param removeExams 是否删除考试事件
 */
class RemoveAllCalendarEventsUseCase(
    private val calendarEvent: CalendarEventPort,
) {
    suspend operator fun invoke(
        removeCourses: Boolean = true,
        removeExams: Boolean = true,
    ): CalendarEventPort.SyncResult {
        return when {
            removeCourses && removeExams -> calendarEvent.removeAllEvents()
            removeCourses -> calendarEvent.removeAllCourseEvents()
            removeExams -> calendarEvent.removeAllExamEvents()
            else -> CalendarEventPort.SyncResult.Success(0)
        }
    }
}
