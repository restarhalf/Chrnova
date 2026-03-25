package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.port.SettingsPort

class ObserveCourseReminderEnabledUseCase(
    private val settings: SettingsPort,
) {
    operator fun invoke(): Flow<Boolean> = settings.observeCourseReminderEnabled()
}
