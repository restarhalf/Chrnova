package restarhalf.stellar.schedule.domain.reminder

object ReminderTiming {

    /** 默认提前 16 分钟，可在提醒设置页调整。 */
    const val LEAD_MINUTES = 16

    /** 提前分钟数可选范围。 */
    val LEAD_MINUTES_RANGE = 1..60

    /** 次日提醒默认 21:00（当天 0 点起的分钟数）。 */
    const val NEXT_DAY_MINUTE_OF_DAY = 21 * 60

    /** 一天内的分钟数范围。 */
    val MINUTES_OF_DAY_RANGE = 0 until 24 * 60
}
