package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot

interface TimetablePort {
    fun getCampus(): Campus
    fun setCampus(campus: Campus)

    fun getCampusTimetable(campus: Campus = getCampus()): List<TimetableSlot>

    fun getTermStartMs(): Long
    fun setTermStartMs(ms: Long)

    fun getTotalWeeks(): Int
    fun setTotalWeeks(weeks: Int)
}
