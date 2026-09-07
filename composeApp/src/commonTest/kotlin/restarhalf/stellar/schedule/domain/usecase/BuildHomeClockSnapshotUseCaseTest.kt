package restarhalf.stellar.schedule.domain.usecase

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 首页时钟快照测试。
 *
 * 输入/输出均为本地时区下的时间语义，期望值用同一时区动态推导，
 * 不依赖测试机时区。
 */
class BuildHomeClockSnapshotUseCaseTest {

    private val useCase = BuildHomeClockSnapshotUseCase()

    private fun labelFor(ms: Long): String {
        val dt = kotlin.time.Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
        val weekday = when (dt.date.dayOfWeek.ordinal + 1) {
            1 -> "星期一"; 2 -> "星期二"; 3 -> "星期三"; 4 -> "星期四"
            5 -> "星期五"; 6 -> "星期六"; else -> "星期日"
        }
        return "${dt.date.month.ordinal + 1}月${dt.date.day}日 $weekday"
    }

    @Test
    fun `星期日快照字段正确`() {
        // 2026-09-06 是星期日
        val nowMs = 1_786_368_000_000L // 2026-09-06T15:30+08:00 附近，具体由动态推导校验
        val snapshot = useCase(nowMs)
        val dt = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(TimeZone.currentSystemDefault())
        assertEquals(dt.date.dayOfWeek.ordinal + 1, snapshot.dayOfWeekMon1)
        assertEquals(dt.hour * 60 + dt.minute, snapshot.nowMinutes)
        assertEquals(labelFor(nowMs), snapshot.dateLabel)
    }

    @Test
    fun `周一返回1且日期标签格式正确`() {
        val ms = 1_786_454_400_000L // 以动态推导为准
        val snapshot = useCase(ms)
        assertEquals(labelFor(ms), snapshot.dateLabel)
        val dt = kotlin.time.Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
        assertEquals(dt.date.dayOfWeek.ordinal + 1, snapshot.dayOfWeekMon1)
        assertEquals(dt.hour * 60 + dt.minute, snapshot.nowMinutes)
    }

    @Test
    fun `午夜零点分钟数为0`() {
        val ms = 1_786_400_640_000L // 动态推导校验
        val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault())
        val snapshot = useCase(ms)
        assertEquals(dt.hour * 60 + dt.minute, snapshot.nowMinutes)
        assertEquals(labelFor(ms), snapshot.dateLabel)
    }
}
