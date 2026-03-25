package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.SettingsPort

class ShouldAutoSyncAndMarkUseCase(
    private val settings: SettingsPort,
) {
    suspend operator fun invoke(nowMs: Long): Boolean = settings.shouldAutoSyncAndMark(nowMs)
}
