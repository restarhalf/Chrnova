package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.first
import restarhalf.stellar.schedule.domain.port.SettingsPort

class SetSelectedTermUseCase(
    private val settings: SettingsPort,
) {
    suspend operator fun invoke(term: String) {
        settings.setSelectedTerm(term)
        if (term.isNotBlank()) {
            settings.setActiveScheduleTerm(term)
        } else {
            val currentTermId = settings.observeCurrentTermId().first()
            settings.setActiveScheduleTerm(currentTermId)
        }
    }
}
