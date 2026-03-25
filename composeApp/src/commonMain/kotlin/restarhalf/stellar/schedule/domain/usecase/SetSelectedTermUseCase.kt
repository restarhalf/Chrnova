package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

class SetSelectedTermUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke(term: String) {
        settings.setSelectedTerm(term)
    }
}
