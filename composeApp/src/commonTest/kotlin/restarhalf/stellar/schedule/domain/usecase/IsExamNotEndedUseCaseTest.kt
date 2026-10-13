package restarhalf.stellar.schedule.domain.usecase

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsExamNotEndedUseCaseTest {

    private val useCase = IsExamNotEndedUseCase()

    private fun msOf(iso: String): Long =
        LocalDateTime.parse(iso).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    @Test
    fun `考试进行中返回true`() {
        val raw = "2026-01-15 14:00-16:00"
        assertTrue(useCase(raw, msOf("2026-01-15T15:00")))
    }

    @Test
    fun `结束时间点恰好相等返回true`() {
        val raw = "2026-01-15 14:00-16:00"
        assertTrue(useCase(raw, msOf("2026-01-15T16:00")))
    }

    @Test
    fun `考试已结束返回false`() {
        val raw = "2026-01-15 14:00-16:00"
        assertFalse(useCase(raw, msOf("2026-01-15T16:01")))
    }

    @Test
    fun `考试尚未开始返回true`() {
        val raw = "2026-01-15 14:00-16:00"
        assertTrue(useCase(raw, msOf("2026-01-15T08:00")))
    }

    @Test
    fun `个位小时时间正常解析`() {
        val raw = "2026-01-15 9:00-10:30"
        assertTrue(useCase(raw, msOf("2026-01-15T10:00")))
        assertFalse(useCase(raw, msOf("2026-01-15T10:31")))
    }

    @Test
    fun `无法解析日期时容错返回true`() {
        assertTrue(useCase("时间待定", msOf("2026-01-15T10:00")))
    }

    @Test
    fun `无法解析结束时间时容错返回true`() {
        assertTrue(useCase("2026-01-15 上午场", msOf("2026-01-15T10:00")))
    }
}
