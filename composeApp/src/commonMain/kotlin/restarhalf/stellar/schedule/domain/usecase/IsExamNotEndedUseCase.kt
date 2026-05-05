package restarhalf.stellar.schedule.domain.usecase

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

class IsExamNotEndedUseCase {
    @OptIn(ExperimentalTime::class)
    operator fun invoke(rawTime: String, nowMs: Long): Boolean {
        val date =
            Regex("(\\d{4}-\\d{2}-\\d{2})").find(rawTime)?.groupValues?.getOrNull(1)
                ?: return true
        val end =
            Regex("~\\s*(\\d{1,2}:\\d{2})").find(rawTime)?.groupValues?.getOrNull(1)
                ?: return true
        val normalized = "${date}T${end.padStart(5, '0')}"
        val endDateTime = runCatching { LocalDateTime.parse(normalized) }.getOrNull() ?: return true
        val endMs = endDateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        return nowMs <= endMs
    }
}
