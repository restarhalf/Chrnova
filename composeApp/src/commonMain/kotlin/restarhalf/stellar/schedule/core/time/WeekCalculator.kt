package restarhalf.stellar.schedule.core.time

import kotlin.time.ExperimentalTime

data class WeekCalcResult(val isHoliday: Boolean, val week: Int, val diffDays: Int)

object WeekCalculator {
    @OptIn(ExperimentalTime::class)
    fun detect(
        totalWeeks: Int,
        termStartMs: Long,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): WeekCalcResult {
        val dayMs = 24L * 60L * 60L * 1000L
        val diffDays = ((nowMs - termStartMs) / dayMs).toInt()
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
