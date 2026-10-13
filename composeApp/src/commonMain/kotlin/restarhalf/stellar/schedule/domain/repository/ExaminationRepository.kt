package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination


interface ExaminationRepository {
    
    fun observeAllExaminations(): Flow<List<Examination>>

    
    fun observeExaminationsByUserNo(userNo: String): Flow<List<Examination>>

    
    fun observeExaminationById(id: Long): Flow<Examination?>

    
    suspend fun saveExamination(examination: Examination, semesterId: String): Long

    
    suspend fun deleteExamination(id: Long)

    
    suspend fun replaceExaminations(semesterId: String, examinations: List<Examination>)

    
    suspend fun clearAll()

    
    suspend fun bindUnboundExaminations(userNo: String)

    
    suspend fun fixInvalidSemesterIds(correctSemesterId: String)
}
