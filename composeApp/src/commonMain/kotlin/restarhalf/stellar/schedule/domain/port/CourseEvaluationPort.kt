package restarhalf.stellar.schedule.domain.port

import restarhalf.stellar.schedule.domain.model.CourseEvaluationSummary
import restarhalf.stellar.schedule.domain.model.Evaluation
import restarhalf.stellar.schedule.domain.model.EvaluationCreateRequest
import restarhalf.stellar.schedule.domain.model.EvaluationPage
import restarhalf.stellar.schedule.domain.model.EvaluationUpdateRequest
import restarhalf.stellar.schedule.domain.model.LikeResult


interface CourseEvaluationPort {
    
    suspend fun listEvaluations(
        course: String? = null,
        teacher: String? = null,
        page: Int = 1,
        size: Int = 20,
    ): EvaluationPage

    
    suspend fun listCourseSummaries(): List<CourseEvaluationSummary>

    
    suspend fun getEvaluation(id: String): Evaluation

    
    suspend fun createEvaluation(req: EvaluationCreateRequest): Evaluation

    
    suspend fun deleteEvaluation(id: String): Boolean

    
    suspend fun updateEvaluation(id: String, req: EvaluationUpdateRequest): Evaluation

    
    suspend fun toggleLike(id: String): LikeResult

    companion object {
        
        @OptIn(dev.whyoleg.cryptography.DelicateCryptographyApi::class)
        fun hashUserNo(userNo: String): String {
            if (userNo.isBlank()) return ""
            val hasher = dev.whyoleg.cryptography.CryptographyProvider.Default
                .get(dev.whyoleg.cryptography.algorithms.SHA256).hasher()
            val digest = hasher.hashBlocking(userNo.encodeToByteArray())
            return digest.toHexString().lowercase()
        }
    }
}
