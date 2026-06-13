package restarhalf.stellar.schedule.core.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

data class WeekCalcResult(val isHoliday: Boolean, val week: Int, val diffDays: Int)

object WeekCalculator {
    @OptIn(ExperimentalTime::class)
    fun detect(
        totalWeeks: Int,
        termStartMs: Long,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): WeekCalcResult {
        val tz = TimeZone.currentSystemDefault()
        val nowDate = kotlin.time.Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val termStartDate = kotlin.time.Instant.fromEpochMilliseconds(termStartMs).toLocalDateTime(tz).date
        val diffDays = termStartDate.daysUntil(nowDate)
        return if (diffDays < 0) {
            WeekCalcResult(isHoliday = true, week = 1, diffDays = diffDays)
        } else {
            val week = diffDays / 7 + 1
            WeekCalcResult(
                isHoliday = week > totalWeeks,
                week = week.coerceIn(1, totalWeeks),
                diffDays = diffDays
            )
        }
    }
}
