package restarhalf.stellar.schedule.core.time

/**
 * 时钟时间工具对象
 * 
 * 提供时间字符串解析功能，用于处理课程表时间配置。
 */
object ClockTime {

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
