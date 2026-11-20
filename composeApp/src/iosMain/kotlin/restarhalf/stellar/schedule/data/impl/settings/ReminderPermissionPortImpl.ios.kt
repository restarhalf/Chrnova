package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ObservableSettings
import restarhalf.stellar.schedule.domain.port.ReminderPermissionPort
import restarhalf.stellar.schedule.domain.port.ReminderPermissionState

class ReminderPermissionPortImpl(
    @Suppress("UNUSED_PARAMETER") settings: ObservableSettings,
) : ReminderPermissionPort {

    override fun snapshot(): ReminderPermissionState =
        ReminderPermissionState(
            notificationGranted = true,
            exactAlarmGranted = true,
            ignoringBatteryOptimizations = true,
            dndGranted = true,
            islandSupported = false,
        )

    override fun openBatteryOptimizationSettings(): Boolean = false

    override fun openAppDetailsSettings(): Boolean = false

    override fun openDndPolicySettings(): Boolean = false
}
