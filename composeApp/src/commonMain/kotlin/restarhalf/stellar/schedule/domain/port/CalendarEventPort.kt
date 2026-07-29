package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.model.Examination
import restarhalf.stellar.schedule.domain.model.TimetableSlot

/**
 * 日历事件端口接口
 *
 * 将课程和考试事件同步到系统日历,由日历应用负责提醒推送。
 * 全量替换语义:每次调用 sync* 会先删除本应用之前写入的事件,再重新写入,
 * 课表/考试变化时调用即可更新,无需手动清理。
 */
interface CalendarEventPort {
    /**
     * 同步课程事件到日历
     *
     * 全量替换:先删除本应用写入的所有课程事件,再为每个上课日写入一个事件。
     * UID 格式:chrnova-course-{courseId}-w{week}@chrnova.local
     *
     * @param courses 课程列表
     * @param termStartMs 学期开始时间戳(毫秒)
     * @param timetable 校区节次时间表
     * @param reminderMinutes 提前提醒分钟数
     */
    suspend fun syncCourseEvents(
        courses: List<Course>,
        termStartMs: Long,
        timetable: List<TimetableSlot>,
        reminderMinutes: Int = 15,
    ): SyncResult

    /**
     * 同步考试事件到日历
     *
     * 全量替换:先删除本应用写入的所有考试事件,再为每场考试写入一个事件。
     * UID 格式:chrnova-exam-{examId}@chrnova.local
     *
     * @param exams 考试列表
     * @param reminderMinutes 提前提醒分钟数
     */
    suspend fun syncExamEvents(
        exams: List<Examination>,
        reminderMinutes: Int = 15,
    ): SyncResult

    /** 删除本应用写入的所有课程事件 */
    suspend fun removeAllCourseEvents(): SyncResult

    /** 删除本应用写入的所有考试事件 */
    suspend fun removeAllExamEvents(): SyncResult

    /** 删除本应用写入的所有事件(课程+考试) */
    suspend fun removeAllEvents(): SyncResult

    /** 检查是否有日历读写权限 */
    fun hasCalendarPermission(): Boolean

    /**
     * 同步结果
     */
    sealed class SyncResult {
        /** 成功,可能包含写入/删除的事件数 */
        data class Success(val affected: Int = 0) : SyncResult()
        /** 失败 */
        data class Failed(val message: String) : SyncResult()
        /** 用户未授权日历权限 */
        data object PermissionDenied : SyncResult()
    }
}
