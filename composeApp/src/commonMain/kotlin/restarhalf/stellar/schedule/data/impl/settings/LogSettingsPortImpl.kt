package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.LogSettingsPort

@OptIn(ExperimentalSettingsApi::class)
class LogSettingsPortImpl(
    private val settings: ObservableSettings,
) : LogSettingsPort {

    override fun observeLogEnabled(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.LOG_ENABLED, false)
    }

    override fun setLogEnabled(enabled: Boolean) {
        settings[SettingsKeys.LOG_ENABLED] = enabled
    }
}
