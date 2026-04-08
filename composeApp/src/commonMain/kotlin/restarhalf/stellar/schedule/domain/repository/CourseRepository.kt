package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Course

interface CourseRepository {
    fun observeAllCourses(): Flow<List<Course>>

    fun observeCourseById(id: Long): Flow<Course?>

    suspend fun insertCourse(course: Course): Long

    suspend fun deleteCourse(course: Course)

    suspend fun getAllCoursesOnce(): List<Course>

    suspend fun replaceSyncedCourses(courses: List<Course>, semesterId: String)

    suspend fun clearAllCourses()
}
