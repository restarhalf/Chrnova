package restarhalf.stellar.schedule.reminder.impl

import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort

class ExactAlarmPermissionPortImpl : ExactAlarmPermissionPort {

    override fun isExactAlarmAllowed(): Boolean = true

    override fun openExactAlarmSettings(): Boolean = false
}
