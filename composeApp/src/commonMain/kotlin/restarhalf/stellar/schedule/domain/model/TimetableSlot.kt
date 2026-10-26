package restarhalf.stellar.schedule.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class TimetableSlot(
    
    val num: Int,
    
    val start: String,
    
    val end: String,
)
