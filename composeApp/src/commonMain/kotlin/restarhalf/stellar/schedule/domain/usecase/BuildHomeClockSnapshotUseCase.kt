package restarhalf.stellar.schedule.domain.usecase

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * 构建首页时钟快照用例
 *
 * 根据当前时间戳生成时钟显示所需的数据快照。
 */
class BuildHomeClockSnapshotUseCase {

    /**
     * 时钟快照
     *
     * @param dayOfWeekMon1 星期几（1=周一，7=周日）
     * @param nowMinutes 当天已过的分钟数
     * @param dateLabel 日期标签，如"6月13日 星期六"
     */
    data class Snapshot(
        val dayOfWeekMon1: Int,
        val nowMinutes: Int,
        val dateLabel: String,
    )

    /**
     * 构建时钟快照
     *
     * @param nowMs 当前时间戳（毫秒）
     * @return 时钟快照
     */
    @OptIn(ExperimentalTime::class)
    operator fun invoke(nowMs: Long): Snapshot {
        val dateTime =
            Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(TimeZone.currentSystemDefault())
        val dayOfWeekMon1 = dateTime.date.dayOfWeek.ordinal + 1
        val nowMinutes = dateTime.hour * 60 + dateTime.minute
        val weekday =
            when (dayOfWeekMon1) {
                1 -> "星期一"
                2 -> "星期二"
                3 -> "星期三"
                4 -> "星期四"
                5 -> "星期五"
                6 -> "星期六"
                else -> "星期日"
            }
        val month = dateTime.date.month.ordinal + 1
        val day = dateTime.date.day
        val dateLabel = "${month}月${day}日 $weekday"
        return Snapshot(
            dayOfWeekMon1 = dayOfWeekMon1,
            nowMinutes = nowMinutes,
            dateLabel = dateLabel,
        )
    }
}
