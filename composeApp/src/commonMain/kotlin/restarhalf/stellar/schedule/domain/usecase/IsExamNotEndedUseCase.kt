package restarhalf.stellar.schedule.domain.usecase

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import restarhalf.stellar.schedule.core.log.AppLogger
import kotlin.time.ExperimentalTime

/**
 * 检查考试是否未结束用例
 * 
 * 解析考试时间字符串，判断考试是否还未结束。
 */
class IsExamNotEndedUseCase {
    /**
     * 检查考试是否未结束
     * 
     * @param rawTime 原始考试时间字符串（如"2024-01-15 ~ 14:00-16:00"）
     * @param nowMs 当前时间戳（毫秒）
     * @return 如果考试未结束返回true
     */
    @OptIn(ExperimentalTime::class)
    operator fun invoke(rawTime: String, nowMs: Long): Boolean {
        // 解析日期
        val date =
            Regex("(\\d{4}-\\d{2}-\\d{2})").find(rawTime)?.groupValues?.getOrNull(1)
                ?: return true
        // 解析结束时间
        val end =
            Regex("~\\s*(\\d{1,2}:\\d{2})").find(rawTime)?.groupValues?.getOrNull(1)
                ?: return true
        val normalized = "${date}T${end.padStart(5, '0')}"
        val endDateTime = runCatching { LocalDateTime.parse(normalized) }
            .onFailure {
                AppLogger.log("Exams", "解析考试结束时间失败: raw=$rawTime", it)
            }
            .getOrNull() ?: return true
        val endMs = endDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        return nowMs <= endMs
    }
}
