package restarhalf.stellar.schedule.reminder.impl

import android.content.Context
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.reminder.ReminderWorkScheduler

class WorkManagerReminderSchedulerPortImpl(
    private val context: Context,
) : ReminderSchedulerPort {
    override fun scheduleNow() {
        ReminderWorkScheduler.enqueueNow(context)
    }
}
