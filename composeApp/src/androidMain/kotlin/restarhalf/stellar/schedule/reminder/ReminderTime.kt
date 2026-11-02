package restarhalf.stellar.schedule.reminder

import java.util.Calendar

internal const val TIMER_TYPE_COUNT_DOWN_RUN = -1

internal fun parseTodayTimeMs(time: String): Long? {
    val parts = time.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].trim().toIntOrNull() ?: return null
    val minute = parts[1].trim().toIntOrNull() ?: return null

    val calendar =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    return calendar.timeInMillis
}

internal data class ReminderStatusLabels(
    val beforeStart: String,
    val beforeEnd: String,
    val noEnd: String,
    val afterEnd: String,
)

internal data class ClockStage(
    val label: String,
    val timerWhenMs: Long = 0L,
) {
    val timerType: Int get() = if (timerWhenMs > 0L) TIMER_TYPE_COUNT_DOWN_RUN else 0
}

internal fun clockStage(
    startAt: Long?,
    endAt: Long?,
    nowMs: Long,
    labels: ReminderStatusLabels,
): ClockStage =
    when {
        startAt == null -> ClockStage(label = "")
        nowMs < startAt -> ClockStage(label = labels.beforeStart, timerWhenMs = startAt)
        endAt != null && nowMs < endAt -> ClockStage(label = labels.beforeEnd, timerWhenMs = endAt)
        endAt == null -> ClockStage(label = labels.noEnd, timerWhenMs = startAt)
        else -> ClockStage(label = labels.afterEnd)
    }
