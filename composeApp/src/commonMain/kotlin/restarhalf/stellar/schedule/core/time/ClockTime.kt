package restarhalf.stellar.schedule.core.time

import kotlinx.datetime.DayOfWeek

/**
 * 时钟时间工具对象
 *
 * 提供时间字符串解析和星期映射功能。
 */
object ClockTime {

    private val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private val WEEKDAY_SHORT = listOf("一", "二", "三", "四", "五", "六", "日")

    /**
     * 将星期数（1-7）转换为中文全称
     */
    fun weekdayText(dayOfWeek: Int): String =
        WEEKDAY_NAMES.getOrElse(dayOfWeek - 1) { "周一" }

    /**
     * 将 [DayOfWeek] 枚举转换为中文单字
     */
    fun weekdayShort(dayOfWeek: DayOfWeek): String =
        WEEKDAY_SHORT[dayOfWeek.ordinal]

    /**
     * 星期列表（周一至周日）
     */
    val weekDays: List<String> get() = WEEKDAY_NAMES

    /**
     * 将时间字符串解析为分钟数
     *
     * @param time 时间字符串，格式为"HH:mm"（如"08:30"）
     * @return 从午夜开始的分钟数，解析失败返回null
     */
    fun parseToMinutes(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    /**
     * 将时间字符串解析为小时和分钟
     *
     * @param time 时间字符串，格式为"HH:mm"（如"08:30"）
     * @return 小时和分钟的Pair，解析失败返回null
     */
    fun parseToHourMinute(time: String): Pair<Int, Int>? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour to minute
    }
}
