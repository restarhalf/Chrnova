package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.port.PapersPort

class FetchPaperDetailUseCase(
    private val papersPort: PapersPort,
) {
    suspend operator fun invoke(id: String): Paper = papersPort.getPaper(id)
}