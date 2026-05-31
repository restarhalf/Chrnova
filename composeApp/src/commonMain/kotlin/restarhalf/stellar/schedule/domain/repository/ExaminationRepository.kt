package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination

interface ExaminationRepository {
    fun observeAllExaminations(): Flow<List<Examination>>
    suspend fun replaceExaminations(semesterId: String, examinations: List<Examination>)
    suspend fun clearAll()
}
