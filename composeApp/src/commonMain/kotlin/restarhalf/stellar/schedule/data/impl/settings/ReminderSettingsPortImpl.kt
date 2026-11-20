package restarhalf.stellar.schedule.data.impl.settings

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getStringFlow
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.SettingsKeys
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort
import restarhalf.stellar.schedule.domain.reminder.ClassroomLabel
import restarhalf.stellar.schedule.domain.reminder.ReminderTiming

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

    override fun observeReminderLeadMinutes(): Flow<Int> =
        settings.getIntFlow(
            key = SettingsKeys.REMINDER_LEAD_MINUTES,
            defaultValue = ReminderTiming.LEAD_MINUTES,
        )

    override fun setReminderLeadMinutes(minutes: Int) {
        settings[SettingsKeys.REMINDER_LEAD_MINUTES] =
            minutes.coerceIn(ReminderTiming.LEAD_MINUTES_RANGE)
    }

    override fun observeExamReminderEnabled(): Flow<Boolean> =
        // 考试提醒沿用课程提醒：总开关开就排考试提醒，不再单独存一份
        settings.getBooleanFlow(SettingsKeys.REMINDER_ENABLED, false)

    override fun observeNextDayReminderEnabled(): Flow<Boolean> =
        settings.getBooleanFlow(SettingsKeys.NEXT_DAY_REMINDER_ENABLED, false)

    override fun setNextDayReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.NEXT_DAY_REMINDER_ENABLED] = enabled
    }

    override fun observeNextDayReminderMinuteOfDay(): Flow<Int> =
        settings.getIntFlow(
            key = SettingsKeys.NEXT_DAY_REMINDER_MINUTE_OF_DAY,
            defaultValue = ReminderTiming.NEXT_DAY_MINUTE_OF_DAY,
        )

    override fun setNextDayReminderMinuteOfDay(minuteOfDay: Int) {
        settings[SettingsKeys.NEXT_DAY_REMINDER_MINUTE_OF_DAY] =
            minuteOfDay.coerceIn(ReminderTiming.MINUTES_OF_DAY_RANGE)
    }

    override fun observeInClassReminderEnabled(): Flow<Boolean> =
        settings.getBooleanFlow(SettingsKeys.IN_CLASS_REMINDER_ENABLED, false)

    override fun setInClassReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.IN_CLASS_REMINDER_ENABLED] = enabled
    }

    override fun observeInClassTimingMode(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.IN_CLASS_TIMING_MODE, defaultValue = 0)

    override fun setInClassTimingMode(mode: Int) {
        settings[SettingsKeys.IN_CLASS_TIMING_MODE] = mode.coerceIn(0, 1)
    }

    override fun observeInClassLeadMinutes(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.IN_CLASS_LEAD_MINUTES, defaultValue = 10)

    override fun setInClassLeadMinutes(minutes: Int) {
        settings[SettingsKeys.IN_CLASS_LEAD_MINUTES] = minutes.coerceIn(0, 60)
    }

    override fun observeClassDndEnabled(): Flow<Boolean> =
        settings.getBooleanFlow(SettingsKeys.CLASS_DND_ENABLED, false)

    override fun setClassDndEnabled(enabled: Boolean) {
        settings[SettingsKeys.CLASS_DND_ENABLED] = enabled
    }

    override fun observeClassDndMode(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.CLASS_DND_MODE, defaultValue = 0)

    override fun setClassDndMode(mode: Int) {
        settings[SettingsKeys.CLASS_DND_MODE] = mode.coerceIn(0, 2)
    }

    override fun observeInExamReminderEnabled(): Flow<Boolean> =
        settings.getBooleanFlow(SettingsKeys.IN_EXAM_REMINDER_ENABLED, false)

    override fun setInExamReminderEnabled(enabled: Boolean) {
        settings[SettingsKeys.IN_EXAM_REMINDER_ENABLED] = enabled
    }

    override fun observeInExamTimingMode(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.IN_EXAM_TIMING_MODE, defaultValue = 0)

    override fun setInExamTimingMode(mode: Int) {
        settings[SettingsKeys.IN_EXAM_TIMING_MODE] = mode.coerceIn(0, 1)
    }

    override fun observeInExamLeadMinutes(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.IN_EXAM_LEAD_MINUTES, defaultValue = 10)

    override fun setInExamLeadMinutes(minutes: Int) {
        settings[SettingsKeys.IN_EXAM_LEAD_MINUTES] = minutes.coerceIn(0, 60)
    }

    override fun observeExamDndEnabled(): Flow<Boolean> =
        settings.getBooleanFlow(SettingsKeys.EXAM_DND_ENABLED, false)

    override fun setExamDndEnabled(enabled: Boolean) {
        settings[SettingsKeys.EXAM_DND_ENABLED] = enabled
    }

    override fun observeExamDndMode(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.EXAM_DND_MODE, defaultValue = 0)

    override fun setExamDndMode(mode: Int) {
        settings[SettingsKeys.EXAM_DND_MODE] = mode.coerceIn(0, 2)
    }

    override fun observeIslandEnabled(): Flow<Boolean> =
        settings.getBooleanFlow(SettingsKeys.ISLAND_NOTIFICATION_ENABLED, true)

    override fun setIslandEnabled(enabled: Boolean) {
        settings[SettingsKeys.ISLAND_NOTIFICATION_ENABLED] = enabled
    }

    override fun observeIslandLeftMode(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.ISLAND_LEFT_MODE, defaultValue = 0)

    override fun setIslandLeftMode(mode: Int) {
        settings[SettingsKeys.ISLAND_LEFT_MODE] = mode.coerceIn(0, 2)
    }

    override fun observeIslandRightMode(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.ISLAND_RIGHT_MODE, defaultValue = 1)

    override fun setIslandRightMode(mode: Int) {
        settings[SettingsKeys.ISLAND_RIGHT_MODE] = mode.coerceIn(0, 2)
    }

    override fun observeIslandAodMode(): Flow<Int> =
        settings.getIntFlow(key = SettingsKeys.ISLAND_AOD_MODE, defaultValue = 0)

    override fun setIslandAodMode(mode: Int) {
        settings[SettingsKeys.ISLAND_AOD_MODE] = mode.coerceIn(0, 1)
    }

    override fun observeClassroomLabelScope(): Flow<Int> =
        settings.getIntFlow(key = ClassroomLabel.KEY_SCOPE, defaultValue = ClassroomLabel.SCOPE_OFF)

    override fun setClassroomLabelScope(scope: Int) {
        settings[ClassroomLabel.KEY_SCOPE] =
            scope.coerceIn(ClassroomLabel.SCOPE_OFF, ClassroomLabel.SCOPE_BOTH)
    }

    override fun observeClassroomLabelMethod(): Flow<Int> =
        settings.getIntFlow(key = ClassroomLabel.KEY_METHOD, defaultValue = ClassroomLabel.METHOD_REGEX)

    override fun setClassroomLabelMethod(method: Int) {
        settings[ClassroomLabel.KEY_METHOD] =
            method.coerceIn(ClassroomLabel.METHOD_REGEX, ClassroomLabel.METHOD_AI)
    }

    override fun observeClassroomLabelAbbreviations(): Flow<String> =
        settings.getStringFlow(key = ClassroomLabel.KEY_ABBREVIATIONS, defaultValue = "")

    override fun setClassroomLabelAbbreviations(text: String) {
        settings[ClassroomLabel.KEY_ABBREVIATIONS] = text
    }

    override fun observeClassroomLabelOverrides(): Flow<String> =
        settings.getStringFlow(key = ClassroomLabel.KEY_OVERRIDES, defaultValue = "")

    override fun setClassroomLabelOverrides(text: String) {
        settings[ClassroomLabel.KEY_OVERRIDES] = text
    }
}
