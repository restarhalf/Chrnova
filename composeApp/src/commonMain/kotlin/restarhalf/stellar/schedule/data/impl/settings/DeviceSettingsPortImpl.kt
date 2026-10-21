package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.DeviceSettingsPort
import kotlin.random.Random


class DeviceSettingsPortImpl(
    private val settings: ObservableSettings,
) : DeviceSettingsPort {


    override fun getDeviceId(): String {
        val existing = settings.getStringOrNull(SettingsKeys.DEVICE_ID)
        if (!existing.isNullOrEmpty()) return existing
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val id = buildString(32) {
            repeat(32) { append(chars[Random.nextInt(chars.length)]) }
        }
        settings[SettingsKeys.DEVICE_ID] = id
        return id
    }


    override fun getStarVerified(): Boolean {
        return settings.getBoolean(SettingsKeys.STAR_VERIFIED, false)
    }


    override fun setStarVerified(verified: Boolean) {
        settings[SettingsKeys.STAR_VERIFIED] = verified
    }


    override fun getConfirmPrivacy(): Boolean {
        return settings.getBoolean(SettingsKeys.CONFIRM_PRIVACY, false)
    }


    override fun setConfirmPrivacy(confirmed: Boolean) {
        settings[SettingsKeys.CONFIRM_PRIVACY] = confirmed
    }


    override fun getDauLastPingDay(): String? {
        return settings.getStringOrNull(SettingsKeys.DAU_LAST_PING_DAY)
    }


    override fun setDauLastPingDay(day: String) {
        settings[SettingsKeys.DAU_LAST_PING_DAY] = day
    }
}
