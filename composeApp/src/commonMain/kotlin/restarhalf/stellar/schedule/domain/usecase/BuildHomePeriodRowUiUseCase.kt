package restarhalf.stellar.schedule.domain.usecase

import androidx.compose.runtime.Immutable
import restarhalf.stellar.schedule.domain.usecase.BuildHomeTodayScheduleUseCase.PeriodItem

class BuildHomePeriodRowUiUseCase {

    @Immutable
    data class RowUi(
        val primaryText: String,
        val secondaryText: String,
        val isPastOrEmpty: Boolean,
    )

    operator fun invoke(item: PeriodItem, status: String?): RowUi {
        val primaryText = item.course?.name ?: "空闲"
        val location = item.course?.location?.ifBlank { "" }.orEmpty()
        val teacher = item.course?.teacher?.ifBlank { "" }.orEmpty()

        val secondaryText =
            if (item.course == null) {
                "第${item.startSection}-${item.endSection}节"
            } else {
                buildList {
                    add("第${item.startSection}-${item.endSection}节")
                    if (location.isNotBlank()) add(location)
                    if (teacher.isNotBlank()) add(teacher)
                }
                    .joinToString(" | ")
            }

        val isPastOrEmpty = item.course == null || status == "已结束"
        return RowUi(
            primaryText = primaryText,
            secondaryText = secondaryText,
            isPastOrEmpty = isPastOrEmpty
        )
    }
}
