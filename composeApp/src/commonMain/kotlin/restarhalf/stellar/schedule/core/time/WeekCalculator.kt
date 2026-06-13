package restarhalf.stellar.schedule.core.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * 周次计算结果数据类
 * 
 * @param isHoliday 是否为假期（学期开始前或结束后）
 * @param week 当前周次（1开始），假期时返回1
 * @param diffDays 距离学期开始的天数，负数表示学期开始前
 */
data class WeekCalcResult(val isHoliday: Boolean, val week: Int, val diffDays: Int)

/**
 * 周次计算器
 * 
 * 根据学期开始时间和当前时间计算当前是第几周。
 */
object WeekCalculator {
    /**
     * 检测当前周次
     * 
     * @param totalWeeks 学期总周数
     * @param termStartMs 学期开始时间戳（毫秒）
     * @param nowMs 当前时间戳（毫秒），默认为当前系统时间
     * @return 周次计算结果
     */
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
            // 学期还没开始，视为假期
            WeekCalcResult(isHoliday = true, week = 1, diffDays = diffDays)
        } else {
            // 计算当前周次，每周7天
            val week = diffDays / 7 + 1
            WeekCalcResult(
                // 超过总周数视为假期
                isHoliday = week > totalWeeks,
                // 将周次限制在有效范围内
                week = week.coerceIn(1, totalWeeks),
                diffDays = diffDays
            )
        }
    }
}
