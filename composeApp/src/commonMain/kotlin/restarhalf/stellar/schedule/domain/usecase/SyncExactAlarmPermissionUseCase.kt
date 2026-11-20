package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.ExactAlarmPermissionPort

class SyncExactAlarmPermissionUseCase(
    private val exactAlarmPermission: ExactAlarmPermissionPort,
    private val isAnyReminderEnabled: IsAnyReminderEnabledUseCase,
    private val rescheduleAllReminders: RescheduleAllRemindersUseCase,
) {

    suspend operator fun invoke(): Boolean {
        val allowed = exactAlarmPermission.isExactAlarmAllowed()
        val previous = exactAlarmPermission.lastKnownAllowed()
        if (previous == allowed) return false

        if (!allowed) {
            exactAlarmPermission.setLastKnownAllowed(false)
            return false
        }

        val shouldReschedule = previous != true && isAnyReminderEnabled()
        if (shouldReschedule) {
            rescheduleAllReminders()
        }

        exactAlarmPermission.setLastKnownAllowed(true)
        return shouldReschedule
    }
}
