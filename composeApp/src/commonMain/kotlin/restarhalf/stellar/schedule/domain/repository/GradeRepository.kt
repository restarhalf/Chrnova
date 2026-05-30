package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.GradeCourse

interface GradeRepository {
    fun observeGrades(semester: String): Flow<List<GradeCourse>>
    fun observeAllGrades(): Flow<List<GradeCourse>>
    suspend fun replaceGrades(semester: String, grades: List<GradeCourse>)
    suspend fun clearAll()
}
