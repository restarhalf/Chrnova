package restarhalf.stellar.schedule.data.impl

import restarhalf.stellar.schedule.data.local.TimetableSettings
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.policy.getCampusTimetable as campusTimetableOf
import restarhalf.stellar.schedule.domain.port.TimetablePort


class TimetablePortImpl(
    private val prefs: TimetableSettings,
) : TimetablePort {


    override fun getCampus(): Campus = prefs.getCampus()


    override fun setCampus(campus: Campus) {
        prefs.setCampus(campus)
    }


    override fun observeCampus() = prefs.observeCampus()


    override fun getCampusTimetable(campus: Campus): List<TimetableSlot> =
        campusTimetableOf(campus)

    
    override fun getTermStartMs(): Long = prefs.getTermStartMs()

    
    override fun setTermStartMs(ms: Long) {
        prefs.setTermStartMs(ms)
    }

    
    override fun observeTermStartMs() = prefs.observeTermStartMs()

    
    override fun getTotalWeeks(): Int = prefs.getTotalWeeks()

    
    override fun setTotalWeeks(weeks: Int) {
        prefs.setTotalWeeks(weeks)
    }

    
    override fun observeTotalWeeks() = prefs.observeTotalWeeks()
}
