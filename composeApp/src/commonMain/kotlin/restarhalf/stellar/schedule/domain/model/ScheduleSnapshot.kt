package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleSnapshot(
    
    val courses: List<Course>,
    
    val timetable: List<TimetableSlot>,
    val totalWeeks: Int,
    val termStartMs: Long,
)
