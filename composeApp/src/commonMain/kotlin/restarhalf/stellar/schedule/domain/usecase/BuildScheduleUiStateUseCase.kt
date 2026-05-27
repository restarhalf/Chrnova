package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.time.WeekCalculator
import restarhalf.stellar.schedule.domain.model.Campus
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import kotlin.time.ExperimentalTime

class BuildScheduleUiStateUseCase(
    private val getCampusTimetable: GetCampusTimetableUseCase,
) {
    data class DetectedWeekInfo(
        val isHoliday: Boolean,
        val week: Int,
        val diffDays: Int,
    )

    data class ScheduleUiState(
        val detectedWeekInfo: DetectedWeekInfo,
        val includeWeek0: Boolean,
        val pagerInitialPage: Int,
        val pagerPageCount: Int,
        val timetable: List<TimetableSlot>,
    )

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
        val timetable = getCampusTimetable(campus)
        return ScheduleUiState(
            detectedWeekInfo = detected,
            includeWeek0 = includeWeek0,
            pagerInitialPage = pagerInitialPage,
            pagerPageCount = pagerPageCount,
            timetable = timetable
        )
    }

    fun pageToWeek(page: Int, includeWeek0: Boolean): Int {
        return if (includeWeek0) page else page + 1
    }

    fun weekToPage(week: Int, includeWeek0: Boolean): Int {
        return if (includeWeek0) week else week - 1
    }
}
