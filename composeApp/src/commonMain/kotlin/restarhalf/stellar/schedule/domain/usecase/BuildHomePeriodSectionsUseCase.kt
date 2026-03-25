package restarhalf.stellar.schedule.domain.usecase

class BuildHomePeriodSectionsUseCase {

    data class SectionUi(
        val title: String,
        val items: List<BuildHomeTodayScheduleUseCase.PeriodItem>,
    )

    operator fun invoke(schedule: BuildHomeTodayScheduleUseCase.HomeTodaySchedule): List<SectionUi> {
        return listOf(
            SectionUi(title = "上午课程", items = schedule.morningItems),
            SectionUi(title = "下午课程", items = schedule.afternoonItems),
            SectionUi(title = "晚上课程", items = schedule.eveningItems),
        )
    }
}
