package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.Paper

interface PapersPort {
    
    suspend fun listPapers(): List<Paper>

    suspend fun getCourses(): List<String>

    suspend fun getFolders(): List<String>

    suspend fun getPaper(id: String): Paper

    suspend fun downloadPaper(id: String): String

    suspend fun uploadPaper(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        title: String,
        folder: String,
    ): Paper

    suspend fun deletePaper(id: String): Boolean

    suspend fun verifyStar(username: String): Boolean
}
