package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.port.CalendarEventPort
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.port.TimetablePort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * 同步课程事件到日历用例
 *
 * 全量写入:先删除本应用之前写入的所有课程事件,再按当前课表重新写入。
 * 若未开启"课程日历提醒",直接返回 Success(0)。
 */
class SyncCourseEventsToCalendarUseCase(
    private val courseRepository: CourseRepository,
    private val timetable: TimetablePort,
    private val calendarEvent: CalendarEventPort,
    private val settings: SettingsPort,
) {
    /**
     * @param campus 当前校区
     * @param termStartMs 学期开始时间戳
     * @param totalWeeks 学期总周数(目前未直接使用,保留接口对齐)
     */
    suspend operator fun invoke(
        campus: Campus,
        termStartMs: Long,
        totalWeeks: Int,
    ): CalendarEventPort.SyncResult {
        val enabled = settings.observeCourseReminderEnabled().first()
        if (!enabled) return CalendarEventPort.SyncResult.Success(0)
        if (!calendarEvent.hasCalendarPermission()) {
            return CalendarEventPort.SyncResult.PermissionDenied
        }
        val courses = courseRepository.getAllCoursesOnce()
        val slots = timetable.getCampusTimetable(campus)
        return calendarEvent.syncCourseEvents(
            courses = courses,
            termStartMs = termStartMs,
            timetable = slots,
        )
    }
}
