package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.PapersPort
import restarhalf.stellar.schedule.domain.port.SettingsPort

class VerifyGitHubStarUseCase(
    private val papersPort: PapersPort,
    private val settingsPort: SettingsPort,
) {
    fun isStarVerified(): Boolean = settingsPort.getStarVerified()

    suspend operator fun invoke(username: String): Boolean {
        val starred = papersPort.verifyStar(username)
        if (starred) {
            settingsPort.setStarVerified(true)
        }
        return starred
    }
}