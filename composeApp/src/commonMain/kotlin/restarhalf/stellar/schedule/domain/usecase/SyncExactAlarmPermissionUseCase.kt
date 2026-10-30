package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort
import restarhalf.stellar.schedule.domain.port.ReminderSettingsPort

class SyncExactAlarmPermissionUseCase(
    private val exactAlarmPermission: ExactAlarmPermissionPort,
    private val reminderSettings: ReminderSettingsPort,
    private val isAnyReminderEnabled: IsAnyReminderEnabledUseCase,
    private val rescheduleAllReminders: RescheduleAllRemindersUseCase,
) {

    /**
     * 每次回到前台调一次：授权从「不允许」变成「允许」且提醒开着时强制重排，
     * 把之前排上的模糊闹钟换成精确闹钟。返回是否发生了重排。
     */
    suspend operator fun invoke(): Boolean {
        val allowed = exactAlarmPermission.isExactAlarmAllowed()
        val previous = reminderSettings.getExactAlarmAllowed()
        if (previous == allowed) return false

        if (!allowed) {
            reminderSettings.setExactAlarmAllowed(false)
            return false
        }

        val shouldReschedule = previous != true && isAnyReminderEnabled()
        if (shouldReschedule) {
            rescheduleAllReminders()
            AppLogger.log("Reminder", "精确闹钟已授权，重新排提醒")
        }

        reminderSettings.setExactAlarmAllowed(true)
        return shouldReschedule
    }
}
