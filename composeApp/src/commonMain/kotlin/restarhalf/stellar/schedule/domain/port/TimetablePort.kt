package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import kotlinx.coroutines.flow.Flow

interface TimetablePort {
    fun getCampus(): Campus
    fun setCampus(campus: Campus)
    fun observeCampus(): Flow<Campus>

    fun getCampusTimetable(campus: Campus = getCampus()): List<TimetableSlot>

    fun getTermStartMs(): Long
    fun setTermStartMs(ms: Long)
    fun observeTermStartMs(): Flow<Long>

    fun getTotalWeeks(): Int
    fun setTotalWeeks(weeks: Int)
    fun observeTotalWeeks(): Flow<Int>
}
