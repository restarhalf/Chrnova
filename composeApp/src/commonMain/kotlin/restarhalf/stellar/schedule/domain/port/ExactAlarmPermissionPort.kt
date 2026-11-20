package restarhalf.stellar.schedule.domain.port

interface ExactAlarmPermissionPort {

    fun isExactAlarmAllowed(): Boolean

    fun openExactAlarmSettings(): Boolean

    fun hasAsked(): Boolean

    fun markAsked()

    fun lastKnownAllowed(): Boolean?

    fun setLastKnownAllowed(allowed: Boolean)
}
