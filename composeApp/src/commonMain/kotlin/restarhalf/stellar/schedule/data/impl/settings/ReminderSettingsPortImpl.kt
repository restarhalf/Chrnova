package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort


@OptIn(ExperimentalSettingsApi::class)
class ReminderSettingsPortImpl(
    private val settings: ObservableSettings,
) : ReminderSettingsPort {


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
}
