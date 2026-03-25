package restarhalf.stellar.schedule.domain.usecase

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
    ): HeaderUi {
        val greeting =
            buildHomeGreetingUseCase(
                courseCount = courseCount,
                hasFirstClass = hasFirstClass,
                seed = dateLabel.hashCode()
            )
        return HeaderUi(dateLabel = dateLabel, greeting = greeting)
    }
}
