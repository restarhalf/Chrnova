package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination

interface ExaminationRepository {
    fun observeExaminations(semesterId: String): Flow<List<Examination>>
    suspend fun replaceExaminations(semesterId: String, examinations: List<Examination>)
    suspend fun clearAll()
}
