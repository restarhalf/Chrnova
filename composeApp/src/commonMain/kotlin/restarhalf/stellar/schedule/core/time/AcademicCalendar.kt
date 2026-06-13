package restarhalf.stellar.schedule.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * 学术日历工具对象
 * 
 * 提供学期日期计算功能，用于课程表的周次日期显示和今日定位。
 */
object AcademicCalendar {
    /**
     * 获取学期开始所在周的周一日期
     * 
     * @param termStartMs 学期开始时间戳（毫秒）
     * @return 学期第一周的周一日期
     */
    @OptIn(ExperimentalTime::class)
    fun getTermStartMonday(termStartMs: Long): LocalDate {
        val termStart = kotlin.time.Instant.fromEpochMilliseconds(termStartMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dayOfWeek = termStart.dayOfWeek
        // 回退到周一（dayOfWeek.ordinal: Monday=0, Sunday=6）
        return termStart.minus(dayOfWeek.ordinal, kotlinx.datetime.DateTimeUnit.DAY)
    }

    /**
     * 获取指定周次的日期列表
     * 
     * @param week 周次（1开始）
     * @param termStartMs 学期开始时间戳（毫秒）
     * @return 7天的日期字符串列表，格式为"M/d"（如"9/01"）
     */
    fun getWeekDates(
        week: Int,
        termStartMs: Long,
    ): List<String> {
        val weekStart =
            getTermStartMonday(termStartMs).plus((week - 1) * 7, kotlinx.datetime.DateTimeUnit.DAY)

        val dateFormat = LocalDate.Format {
            monthNumber()
            char('/')
            this@Format.day(padding = Padding.ZERO)
        }

        return (0..6).map { dayOffset ->
            val date = weekStart.plus(dayOffset, kotlinx.datetime.DateTimeUnit.DAY)
            date.format(dateFormat)
        }
    }

    /**
     * 获取今日在指定周次中的索引
     * 
     * @param week 周次（1开始）
     * @param termStartMs 学期开始时间戳（毫秒）
     * @param nowMs 当前时间戳（毫秒），默认为当前系统时间
     * @return 今日在该周中的索引（0=周一，6=周日），如果今日不在该周返回null
     */
    @OptIn(ExperimentalTime::class)
    fun getTodayIndexInWeek(
        week: Int,
        termStartMs: Long,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): Int? {
        if (week <= 0) return null
        val weekStart =
            getTermStartMonday(termStartMs).plus((week - 1) * 7, kotlinx.datetime.DateTimeUnit.DAY)
        val today = kotlin.time.Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date

        for (dayIndex in 0..6) {
            val dayDate = weekStart.plus(dayIndex, kotlinx.datetime.DateTimeUnit.DAY)
            if (dayDate == today) {
                return dayIndex
            }
        }
        return null
    }

    /**
     * 获取当前周的周一日期
     * 
     * @param nowMs 当前时间戳（毫秒），默认为当前系统时间
     * @return 当前周的周一日期
     */
    @OptIn(ExperimentalTime::class)
    fun getCurrentWeekMonday(
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): LocalDate {
        val today = kotlin.time.Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.minus(today.dayOfWeek.ordinal, kotlinx.datetime.DateTimeUnit.DAY)
    }

    /**
     * 获取当前周的日期列表
     * 
     * @param dayCount 返回的天数，默认7天
     * @param nowMs 当前时间戳（毫秒），默认为当前系统时间
     * @return 日期字符串列表，格式为"M/d"
     */
    @OptIn(ExperimentalTime::class)
    fun getCurrentWeekDates(
        dayCount: Int = 7,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): List<String> {
        val weekStart = getCurrentWeekMonday(nowMs)
        val dateFormat = LocalDate.Format {
            monthNumber()
            char('/')
            this@Format.day(padding = Padding.ZERO)
        }
        return (0..6)
            .map { dayOffset ->
                val date = weekStart.plus(dayOffset, kotlinx.datetime.DateTimeUnit.DAY)
                date.format(dateFormat)
            }
            .take(dayCount)
    }

    /**
     * 获取今日在当前周中的索引
     * 
     * @param nowMs 当前时间戳（毫秒），默认为当前系统时间
     * @return 今日在当前周中的索引（0=周一，6=周日）
     */
    @OptIn(ExperimentalTime::class)
    fun getTodayIndexInCurrentWeek(
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
    ): Int {
        val today = kotlin.time.Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        return today.dayOfWeek.ordinal
    }
}
