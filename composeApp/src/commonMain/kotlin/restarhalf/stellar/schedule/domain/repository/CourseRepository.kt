package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Course

interface CourseRepository {
    
    fun observeAllCourses(): Flow<List<Course>>

    fun observeCourseById(id: Long): Flow<Course?>

    suspend fun insertCourse(course: Course): Long

    suspend fun insertCourses(courses: List<Course>): List<Long>

    suspend fun deleteCourse(course: Course)

    /** 在同一个事务里删掉 [toDelete]、再插入 [toInsert]，避免删完没插进去丢数据。 */
    suspend fun replaceCourses(toDelete: List<Course>, toInsert: List<Course>)

    suspend fun getAllCoursesOnce(): List<Course>

    suspend fun getAllCoursesAcrossSemesters(): List<Course>

    suspend fun replaceSyncedCourses(courses: List<Course>, semesterId: String)

    suspend fun clearAllCourses()

    suspend fun bindUnboundCourses(userNo: String)
}
