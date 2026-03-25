package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.data.local.TimetableSettings
import restarhalf.stellar.schedule.data.local.getCampusTimetable
import restarhalf.stellar.schedule.data.mapper.toData
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.TimetablePort

class TimetablePortImpl(
    private val prefs: TimetableSettings,
) : TimetablePort {

    override fun getCampus(): Campus = prefs.getCampus().toDomain()

    override fun setCampus(campus: Campus) {
        prefs.setCampus(campus.toData())
    }

    override fun getCampusTimetable(campus: Campus): List<TimetableSlot> {
        return getCampusTimetable(campus.toData()).map { slot ->
            TimetableSlot(num = slot.num, start = slot.start, end = slot.end)
        }
    }

    override fun getTermStartMs(): Long = prefs.getTermStartMs()

    override fun setTermStartMs(ms: Long) {
        prefs.setTermStartMs(ms)
    }

    override fun getTotalWeeks(): Int = prefs.getTotalWeeks()

    override fun setTotalWeeks(weeks: Int) {
        prefs.setTotalWeeks(weeks)
    }
}
