package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.port.PapersPort

class FetchPapersUseCase(
    private val papersPort: PapersPort,
) {
    suspend operator fun invoke(): List<Paper> = papersPort.listPapers()
}