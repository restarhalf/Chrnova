package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Paper
import restarhalf.stellar.schedule.domain.port.PapersPort

class UploadPaperUseCase(
    private val papersPort: PapersPort,
) {
    suspend operator fun invoke(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        title: String,
        folder: String,
    ): Paper = papersPort.uploadPaper(
        fileBytes = fileBytes,
        fileName = fileName,
        mimeType = mimeType,
        title = title,
        folder = folder,
    )
}