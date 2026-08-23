package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.core.update.AppUpdateInfo
import restarhalf.stellar.schedule.core.update.AppUpdatePort

class CheckAppUpdateUseCase(
    private val appUpdate: AppUpdatePort,
    private val getGrayUid: () -> String? = { null },
) {
    suspend operator fun invoke(currentVersionName: String): AppUpdateInfo? =
        appUpdate.check(currentVersionName, getGrayUid())
}
