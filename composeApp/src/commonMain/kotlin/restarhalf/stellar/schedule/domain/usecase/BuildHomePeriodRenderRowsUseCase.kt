package restarhalf.stellar.schedule.domain.usecase

import androidx.compose.runtime.Stable
import restarhalf.stellar.schedule.domain.model.TimetableSlot
import restarhalf.stellar.schedule.domain.usecase.BuildHomePeriodRowUiUseCase.RowUi
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase.PeriodItem

class BuildHomePeriodRenderRowsUseCase(
    private val buildHomeTodayScheduleUseCase: BuildHomeTodayScheduleUseCase,
    private val resolveCourseStatusUseCase: ResolveCourseStatusUseCase,
    private val buildHomePeriodRowUiUseCase: BuildHomePeriodRowUiUseCase,
) {

    @Stable
    data class RowRenderUi(
        val item: PeriodItem,
        val timeRange: Pair<String, String>,
        val status: String?,
        val rowUi: RowUi,
        val accentCourseName: String,
    )

    operator fun invoke(
        items: List<PeriodItem>,
        timetable: List<TimetableSlot>,
        nowMinutes: Int,
    ): List<RowRenderUi> {
        val timeRangeCache = HashMap<Pair<Int, Int>, Pair<String, String>>()
        return items.map { item ->
            val key = item.startSection to item.endSection
            val timeRange = timeRangeCache.getOrPut(key) {
                buildHomeTodayScheduleUseCase.timeRange(
                    timetable = timetable,
                    startSection = item.startSection,
                    endSection = item.endSection
                )
            }
            val status =
                resolveCourseStatusUseCase(
                    hasCourse = item.course != null,
                    startTime = timeRange.first,
                    endTime = timeRange.second,
                    nowMinutes = nowMinutes
                )
            val rowUi = buildHomePeriodRowUiUseCase(item = item, status = status)
            RowRenderUi(
                item = item,
                timeRange = timeRange,
                status = status,
                rowUi = rowUi,
                accentCourseName = item.course?.name.orEmpty()
            )
        }
    }
}
