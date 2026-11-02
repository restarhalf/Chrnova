package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort

class ExactAlarmPermissionPortImpl(
    settings: ObservableSettings,
) : ExactAlarmPermissionPort {

    private val store = ExactAlarmPermissionStore(settings)

    override fun isExactAlarmAllowed(): Boolean = true

    override fun openExactAlarmSettings(): Boolean = false

    override fun hasAsked(): Boolean = store.hasAsked()

    override fun markAsked() = store.markAsked()

    override fun lastKnownAllowed(): Boolean? = store.lastKnownAllowed()

    override fun setLastKnownAllowed(allowed: Boolean) = store.setLastKnownAllowed(allowed)
}
