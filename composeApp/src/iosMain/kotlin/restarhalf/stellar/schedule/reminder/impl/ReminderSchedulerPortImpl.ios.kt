package restarhalf.stellar.schedule.reminder.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.port.ReminderSchedulerPort
import restarhalf.stellar.schedule.domain.usecase.RescheduleRemindersUseCase

class ReminderSchedulerPortImpl(
    private val rescheduleReminders: RescheduleRemindersUseCase,
) : ReminderSchedulerPort {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun scheduleNow() {
        scope.launch {
            runCatching { rescheduleReminders() }
                .onFailure { AppLogger.log("Reminder", "iOS重新调度提醒失败", it) }
        }
    }
}
