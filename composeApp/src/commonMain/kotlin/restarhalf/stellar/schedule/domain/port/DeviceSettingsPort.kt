package restarhalf.stellar.schedule.domain.port


interface DeviceSettingsPort {

    fun getDeviceId(): String


    fun getStarVerified(): Boolean

    fun setStarVerified(verified: Boolean)


    fun getConfirmPrivacy(): Boolean

    fun setConfirmPrivacy(confirmed: Boolean)


    fun getDauLastPingDay(): String?

    fun setDauLastPingDay(day: String)
}
