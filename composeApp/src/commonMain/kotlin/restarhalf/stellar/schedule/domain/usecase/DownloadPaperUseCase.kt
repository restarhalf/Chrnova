package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.port.PapersPort

class DownloadPaperUseCase(
    private val papersPort: PapersPort,
) {
    suspend operator fun invoke(id: String): String = papersPort.downloadPaper(id)
}