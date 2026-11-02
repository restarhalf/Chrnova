package restarhalf.stellar.schedule.reminder.impl

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.set
import restarhalf.stellar.schedule.domain.port.ReminderScheduleResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class IosReminderState(
    private val settings: ObservableSettings,
    private val identifier: String,
    private val scheduledKey: String,
    private val triggerKey: String,
) {

    @OptIn(ExperimentalTime::class)
    suspend fun schedule(title: String, body: String, triggerAtMs: Long): ReminderScheduleResult {
        val success =
            IosReminderNotifications.schedule(
                identifier = identifier,
                title = title,
                body = body,
                triggerAtMs = triggerAtMs,
            )

        if (!success) {
            clear()
            return ReminderScheduleResult.Failed
        }

        settings[scheduledKey] = true
        settings[triggerKey] = triggerAtMs
        return ReminderScheduleResult.Scheduled(triggerAtMs)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun hasScheduled(): Boolean {
        if (!settings.getBoolean(scheduledKey, false)) return false

        val triggerAtMs = settings.getLong(triggerKey, 0L)
        if (triggerAtMs <= 0L || triggerAtMs <= nowMs()) {
            cancelAll()
            return false
        }
        if (IosReminderNotifications.isPending(identifier) == false) {
            clear()
            return false
        }
        return true
    }

    @OptIn(ExperimentalTime::class)
    fun nextScheduledAtMs(): Long? {
        if (!settings.getBoolean(scheduledKey, false)) return null

        val triggerAtMs = settings.getLong(triggerKey, 0L)
        // 通知响过之后预存值会留在原地，过期就当没挂
        if (triggerAtMs <= 0L || triggerAtMs <= nowMs()) return null
        return triggerAtMs
    }

    fun cancelAll() {
        IosReminderNotifications.cancel(identifier)
        clear()
    }

    @OptIn(ExperimentalTime::class)
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun clear() {
        settings[scheduledKey] = false
        settings.remove(triggerKey)
    }
}
