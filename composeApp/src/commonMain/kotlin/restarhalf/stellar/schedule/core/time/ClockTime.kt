package restarhalf.stellar.schedule.core.time

import kotlinx.datetime.DayOfWeek

object ClockTime {

    private val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private val WEEKDAY_SHORT = listOf("一", "二", "三", "四", "五", "六", "日")

    fun weekdayText(dayOfWeek: Int): String =
        WEEKDAY_NAMES.getOrElse(dayOfWeek - 1) { "周一" }

    fun weekdayShort(dayOfWeek: DayOfWeek): String =
        WEEKDAY_SHORT[dayOfWeek.ordinal]

    val weekDays: List<String> get() = WEEKDAY_NAMES

    fun parseToMinutes(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    fun parseToHourMinute(time: String): Pair<Int, Int>? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour to minute
    }
}
