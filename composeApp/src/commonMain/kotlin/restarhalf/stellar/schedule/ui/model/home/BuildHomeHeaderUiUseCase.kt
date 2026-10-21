package restarhalf.stellar.schedule.ui.model.home

class BuildHomeHeaderUiUseCase(
    private val buildHomeGreetingUseCase: BuildHomeGreetingUseCase,
) {

    data class HeaderUi(
        val dateLabel: String,
        val greeting: String,
    )

    operator fun invoke(
        dateLabel: String,
        courseCount: Int,
        hasFirstClass: Boolean,
        dayOfWeekCount: Int,
    ): HeaderUi {
        val greeting =
            buildHomeGreetingUseCase(
                courseCount = courseCount,
                hasFirstClass = hasFirstClass,
                dayOfWeekCount = dayOfWeekCount
            )
        return HeaderUi(dateLabel = dateLabel, greeting = greeting)
    }
}
