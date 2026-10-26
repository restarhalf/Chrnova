package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.GradeCourse

interface GradeRepository {
    
    fun observeAllGrades(): Flow<List<GradeCourse>>

    fun observeGradesByUserNo(userNo: String): Flow<List<GradeCourse>>

    suspend fun getAllGradesByUserNo(userNo: String): List<GradeCourse>

    suspend fun replaceGrades(semester: String, grades: List<GradeCourse>)

    suspend fun replaceGradesByUserNoAndSemester(
        userNo: String,
        semester: String,
        grades: List<GradeCourse>
    )

    suspend fun clearAll()
}
