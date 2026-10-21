package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow


interface ReminderSettingsPort {

    fun observeCourseReminderEnabled(): Flow<Boolean>

    fun setCourseReminderEnabled(enabled: Boolean)


    fun observeExamReminderEnabled(): Flow<Boolean>

    fun setExamReminderEnabled(enabled: Boolean)
}
