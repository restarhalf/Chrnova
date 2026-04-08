package restarhalf.stellar.schedule.data.impl

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.SettingsPort

@OptIn(ExperimentalSettingsApi::class)
class SettingsPortImpl(
    private val settings: ObservableSettings,
) : SettingsPort {

    override fun observeShowNonCurrentWeek(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.SHOW_NON_CURRENT_WEEK, true)
    }

    override fun setShowNonCurrentWeek(show: Boolean) {
        settings[SettingsKeys.SHOW_NON_CURRENT_WEEK] = show
    }

    override fun observeThemeMode(): Flow<Int> {
        return settings.getIntFlow(SettingsKeys.THEME_MODE, 0)
    }

    override fun setThemeMode(mode: Int) {
        settings[SettingsKeys.THEME_MODE] = mode.coerceIn(0, 2)
    }

    override fun observeFloatingBar(): Flow<Int> {
        return settings.getIntFlow(SettingsKeys.FLOATING_BAR, 0)
    }

    override fun setFloatingBar(mode: Int) {
        settings[SettingsKeys.FLOATING_BAR] = mode.coerceIn(0,2)
    }

    override fun observeSelectedTerm(): Flow<String> {
        return settings.getStringFlow(SettingsKeys.SELECTED_TERM, "")
    }

    override fun setSelectedTerm(term: String) {
        settings[SettingsKeys.SELECTED_TERM] = term
    }

    override fun observeActiveScheduleTerm(): Flow<String> {
        return settings.getStringFlow(SettingsKeys.ACTIVE_SCHEDULE_TERM, "")
    }

    override fun setActiveScheduleTerm(term: String) {
        settings[SettingsKeys.ACTIVE_SCHEDULE_TERM] = term
    }

    override fun observeCourseReminderEnabled(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.REMINDER_ENABLED, false)
    }

    override fun setCourseReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.REMINDER_ENABLED] = enabled
    }

    override fun observeExamReminderEnabled(): Flow<Boolean> {
        return settings.getBooleanFlow(SettingsKeys.EXAM_REMINDER_ENABLED, false)
    }

    override fun setExamReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.EXAM_REMINDER_ENABLED] = enabled
    }

    override suspend fun shouldAutoSyncAndMark(nowMs: Long): Boolean {
        val lastAutoSyncMs = settings.getLong(SettingsKeys.LAST_AUTO_SYNC_MS, 0L)
        val cooldownMs = 24L * 60L * 60L * 1000L
        return if (nowMs - lastAutoSyncMs >= cooldownMs) {
            settings[SettingsKeys.LAST_AUTO_SYNC_MS] = nowMs
            true
        } else {
            false
        }
    }
}
