package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.port.TimetablePort
import kotlin.time.ExperimentalTime

/**
 * 构建课程表UI状态用例
 * 
 * 计算课程表页面的初始状态，包括当前周次、页码、时间表等。
 */
class BuildScheduleUiStateUseCase(
    private val timetable: TimetablePort,
) {
    /**
     * 检测到的周次信息
     * 
     * @param isHoliday 是否为假期
     * @param week 当前周次
     * @param diffDays 距离学期开始的天数差
     */
    data class DetectedWeekInfo(
        val isHoliday: Boolean,
        val week: Int,
        val diffDays: Int,
    )

    /**
     * 课程表UI状态
     * 
     * @param detectedWeekInfo 检测到的周次信息
     * @param includeWeek0 是否包含第0周（假期周）
     * @param pagerInitialPage Pager初始页码
     * @param pagerPageCount Pager总页数
     * @param timetable 时间表配置
     */
    data class ScheduleUiState(
        val detectedWeekInfo: DetectedWeekInfo,
        val includeWeek0: Boolean,
        val pagerInitialPage: Int,
        val pagerPageCount: Int,
        val timetable: List<TimetableSlot>,
    )

    /**
     * 检测周次信息
     * 
     * @param totalWeeks 总周数
     * @param termStartMs 学期开始时间戳
     * @param nowMs 当前时间戳
     * @return 检测到的周次信息
     */
    fun detectWeekInfo(
        totalWeeks: Int,
        termStartMs: Long,
        nowMs: Long,
    ): DetectedWeekInfo {
        val result =
            WeekCalculator.detect(totalWeeks = totalWeeks, termStartMs = termStartMs, nowMs = nowMs)
        return DetectedWeekInfo(
            isHoliday = result.isHoliday,
            week = result.week,
            diffDays = result.diffDays
        )
    }

    /**
     * 构建课程表UI状态
     * 
     * @param campus 校区
     * @param totalWeeks 总周数
     * @param termStartMs 学期开始时间戳
     * @param nowMs 当前时间戳
     * @return 课程表UI状态
     */
    @OptIn(ExperimentalTime::class)
    operator fun invoke(
        campus: Campus,
        totalWeeks: Int,
        termStartMs: Long,
        nowMs: Long = kotlin.time.Clock.System.now().toEpochMilliseconds(),
    ): ScheduleUiState {
        val detected =
            detectWeekInfo(totalWeeks = totalWeeks, termStartMs = termStartMs, nowMs = nowMs)
        val includeWeek0 = detected.isHoliday
        val pagerInitialPage = if (includeWeek0) 0 else (detected.week - 1)
        val pagerPageCount = if (includeWeek0) totalWeeks + 1 else totalWeeks
        val timetable = timetable.getCampusTimetable(campus)
        return ScheduleUiState(
            detectedWeekInfo = detected,
            includeWeek0 = includeWeek0,
            pagerInitialPage = pagerInitialPage,
            pagerPageCount = pagerPageCount,
            timetable = timetable
        )
    }

    /**
     * 页面索引转周次
     * 
     * @param page 页面索引
     * @param includeWeek0 是否包含第0周
     * @return 周次
     */
    fun pageToWeek(page: Int, includeWeek0: Boolean): Int {
        return if (includeWeek0) page else page + 1
    }

    /**
     * 周次转页面索引
     * 
     * @param week 周次
     * @param includeWeek0 是否包含第0周
     * @return 页面索引
     */
    fun weekToPage(week: Int, includeWeek0: Boolean): Int {
        return if (includeWeek0) week else week - 1
    }
}
