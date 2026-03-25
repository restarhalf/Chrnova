package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.time.ClockTime

class ResolveCourseStatusUseCase {

    operator fun invoke(
        hasCourse: Boolean,
        startTime: String,
        endTime: String,
        nowMinutes: Int,
    ): String? {
        if (!hasCourse) return null
        val startMin = ClockTime.parseToMinutes(startTime) ?: return null
        val endMin = ClockTime.parseToMinutes(endTime) ?: return null
        return when {
            nowMinutes > endMin -> "已结束"
            nowMinutes < startMin -> "未开始"
            else -> "进行中"
        }
    }
}
