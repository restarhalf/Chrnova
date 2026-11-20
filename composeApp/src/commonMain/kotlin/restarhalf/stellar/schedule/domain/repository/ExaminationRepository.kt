package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Examination

interface ExaminationRepository {
    
    suspend fun getAllExaminationsOnce(): List<Examination>

    fun observeExaminationsByUserNo(userNo: String): Flow<List<Examination>>

    fun observeExaminationById(id: Long): Flow<Examination?>

    suspend fun saveExamination(examination: Examination, semesterId: String): Long

    suspend fun deleteExamination(id: Long)

    suspend fun replaceExaminations(userNo: String, semesterId: String, examinations: List<Examination>)

    suspend fun fixInvalidSemesterIds(correctSemesterId: String)
}
