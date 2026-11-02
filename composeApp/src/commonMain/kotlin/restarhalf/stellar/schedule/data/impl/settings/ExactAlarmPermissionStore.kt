package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import restarhalf.stellar.schedule.domain.model.SettingsKeys

class ExactAlarmPermissionStore(
    private val settings: ObservableSettings,
) {

    fun hasAsked(): Boolean = settings.getBoolean(SettingsKeys.EXACT_ALARM_PERMISSION_ASKED, false)

    fun markAsked() {
        settings[SettingsKeys.EXACT_ALARM_PERMISSION_ASKED] = true
    }

    fun lastKnownAllowed(): Boolean? {
        if (!settings.hasKey(SettingsKeys.EXACT_ALARM_ALLOWED)) return null
        return settings.getBoolean(SettingsKeys.EXACT_ALARM_ALLOWED, false)
    }

    fun setLastKnownAllowed(allowed: Boolean) {
        settings[SettingsKeys.EXACT_ALARM_ALLOWED] = allowed
    }
}
