package restarhalf.stellar.schedule.domain.port

import kotlinx.coroutines.flow.Flow

interface ReminderSettingsPort {

    fun observeCourseReminderEnabled(): Flow<Boolean>

    fun setCourseReminderEnabled(enabled: Boolean)

    fun observeExamReminderEnabled(): Flow<Boolean>

    fun setExamReminderEnabled(enabled: Boolean)

    /** 是否已经就「精确闹钟」权限弹过系统设置页；只弹一次，避免每次冷启动都跳。 */
    fun hasAskedExactAlarmPermission(): Boolean

    fun markExactAlarmPermissionAsked()
}
