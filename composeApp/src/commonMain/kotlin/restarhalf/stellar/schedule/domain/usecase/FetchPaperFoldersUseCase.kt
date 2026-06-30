package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.PapersPort

class FetchPaperFoldersUseCase(
    private val papersPort: PapersPort,
) {
    suspend operator fun invoke(): List<String> = papersPort.getFolders()
}