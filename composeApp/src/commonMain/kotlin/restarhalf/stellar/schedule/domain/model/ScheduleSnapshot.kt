package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable

/**
 * 渲染课表所需的一次性只读快照。
 *
 * 桌面小组件、通知等「进程外的短暂渲染」不适合持有仓库或订阅 Flow，
 * 统一取这一份快照即可。
 */
@Immutable
data class ScheduleSnapshot(
    /** 已按当前生效学期过滤过的课程。 */
    val courses: List<Course>,
    /** 当前校区的作息表。 */
    val timetable: List<TimetableSlot>,
    val totalWeeks: Int,
    val termStartMs: Long,
)
