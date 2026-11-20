package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

interface ReminderSettingsPort {

    fun observeCourseReminderEnabled(): Flow<Boolean>

    fun setCourseReminderEnabled(enabled: Boolean)

    fun observeReminderLeadMinutes(): Flow<Int>

    fun setReminderLeadMinutes(minutes: Int)

    /** 考试提醒沿用课程提醒总开关，没有独立的设置项。 */
    fun observeExamReminderEnabled(): Flow<Boolean>

    fun observeNextDayReminderEnabled(): Flow<Boolean>

    fun setNextDayReminderEnabled(enabled: Boolean)

    fun observeNextDayReminderMinuteOfDay(): Flow<Int>

    fun setNextDayReminderMinuteOfDay(minuteOfDay: Int)

    fun observeInClassReminderEnabled(): Flow<Boolean>

    fun setInClassReminderEnabled(enabled: Boolean)

    fun observeInClassTimingMode(): Flow<Int>

    fun setInClassTimingMode(mode: Int)

    fun observeInClassLeadMinutes(): Flow<Int>

    fun setInClassLeadMinutes(minutes: Int)

    fun observeClassDndEnabled(): Flow<Boolean>

    fun setClassDndEnabled(enabled: Boolean)

    fun observeClassDndMode(): Flow<Int>

    fun setClassDndMode(mode: Int)

    fun observeInExamReminderEnabled(): Flow<Boolean>

    fun setInExamReminderEnabled(enabled: Boolean)

    fun observeInExamTimingMode(): Flow<Int>

    fun setInExamTimingMode(mode: Int)

    fun observeInExamLeadMinutes(): Flow<Int>

    fun setInExamLeadMinutes(minutes: Int)

    fun observeExamDndEnabled(): Flow<Boolean>

    fun setExamDndEnabled(enabled: Boolean)

    fun observeExamDndMode(): Flow<Int>

    fun setExamDndMode(mode: Int)

    fun observeIslandEnabled(): Flow<Boolean>

    fun setIslandEnabled(enabled: Boolean)

    fun observeIslandLeftMode(): Flow<Int>

    fun setIslandLeftMode(mode: Int)

    fun observeIslandRightMode(): Flow<Int>

    fun setIslandRightMode(mode: Int)

    fun observeIslandAodMode(): Flow<Int>

    fun setIslandAodMode(mode: Int)

    fun observeClassroomLabelScope(): Flow<Int>

    fun setClassroomLabelScope(scope: Int)

    fun observeClassroomLabelMethod(): Flow<Int>

    fun setClassroomLabelMethod(method: Int)

    fun observeClassroomLabelAbbreviations(): Flow<String>

    fun setClassroomLabelAbbreviations(text: String)

    fun observeClassroomLabelOverrides(): Flow<String>

    fun setClassroomLabelOverrides(text: String)
}
