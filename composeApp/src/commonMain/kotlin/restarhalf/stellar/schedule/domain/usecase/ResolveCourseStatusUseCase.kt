package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.time.ClockTime

/**
 * 解析课程状态用例
 * 
 * 根据当前时间和课程时间，判断课程的状态（进行中、未开始、已结束）。
 */
class ResolveCourseStatusUseCase {

    /**
     * 解析课程状态
     * 
     * @param hasCourse 是否有课程
     * @param startTime 开始时间（如"08:00"）
     * @param endTime 结束时间（如"08:45"）
     * @param nowMinutes 当前时间的分钟数（从午夜开始）
     * @return 状态文本，无课程时返回null
     */
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
