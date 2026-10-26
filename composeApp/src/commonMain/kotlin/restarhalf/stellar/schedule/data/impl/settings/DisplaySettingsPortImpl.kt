package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.DisplaySettingsPort

@OptIn(ExperimentalSettingsApi::class)
class DisplaySettingsPortImpl(
    private val settings: ObservableSettings,
) : DisplaySettingsPort {

    override fun observeShowNonCurrentWeek(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.SHOW_NON_CURRENT_WEEK, true)
    }

    override fun setShowNonCurrentWeek(show: Boolean) {
        settings[SettingsKeys.SHOW_NON_CURRENT_WEEK] = show
    }

    override fun observeScheduleRowHeight(): Flow<Int> {
        return settings.getIntFlow(
            SettingsKeys.SCHEDULE_ROW_HEIGHT,
            DisplaySettingsPort.DEFAULT_ROW_HEIGHT_DP,
        )
    }

    override fun setScheduleRowHeight(heightDp: Int) {
        settings[SettingsKeys.SCHEDULE_ROW_HEIGHT] = heightDp.coerceIn(
            DisplaySettingsPort.MIN_ROW_HEIGHT_DP,
            DisplaySettingsPort.MAX_ROW_HEIGHT_DP,
        )
    }

    override fun observeThemeMode(): Flow<Int> {
        return settings.getIntFlow(SettingsKeys.THEME_MODE, 0)
    }

    override fun getThemeMode(): Int {
        return settings.getInt(SettingsKeys.THEME_MODE, 0)
    }

    override fun setThemeMode(mode: Int) {
        settings[SettingsKeys.THEME_MODE] = mode.coerceIn(0, 2)
    }

    override fun observeFloatingBar(): Flow<Int> {
        return settings.getIntFlow(SettingsKeys.FLOATING_BAR, 0)
    }

    override fun getFloatingBar(): Int {
        return settings.getInt(SettingsKeys.FLOATING_BAR, 0)
    }

    override fun setFloatingBar(mode: Int) {
        settings[SettingsKeys.FLOATING_BAR] = mode.coerceIn(0, 2)
    }
}
