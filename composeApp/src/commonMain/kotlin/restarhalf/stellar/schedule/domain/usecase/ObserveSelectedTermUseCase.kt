package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.SettingsPort

class ObserveSelectedTermUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke(): Flow<String> = settings.observeSelectedTerm()
}
