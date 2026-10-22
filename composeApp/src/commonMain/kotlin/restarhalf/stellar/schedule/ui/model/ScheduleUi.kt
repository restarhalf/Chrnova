package restarhalf.stellar.schedule.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import restarhalf.stellar.schedule.ui.mapper.DayRenderData
import restarhalf.stellar.schedule.ui.model.home.BuildHomePeriodRenderRowsUseCase

@Stable
data class SectionRenderUi(
    val title: String,
    val rows: ImmutableList<BuildHomePeriodRenderRowsUseCase.RowRenderUi>,
)

@Stable
data class WeekHeaderUi(
    val days: ImmutableList<String>,
    val dates: ImmutableList<String>,
    val todayIndex: Int?,
)

@Stable
data class PageRenderUi(
    val actualWeek: Int,
    val dayRenderData: ImmutableMap<Int, DayRenderData>,
)

enum class CourseDetailTagStyle {
    LAB,
    TRANS,
    NON_CURRENT,
}

@Immutable
data class CourseDetailUi(
    val weekLine: String,
    val locationLine: String,
    val tagText: String?,
    val tagStyle: CourseDetailTagStyle?,
)
