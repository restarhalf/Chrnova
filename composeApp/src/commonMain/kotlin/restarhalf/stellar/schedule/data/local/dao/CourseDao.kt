package restarhalf.stellar.schedule.data.local.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.data.local.entity.CourseEntity

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE userNo = :userNo")
    fun getCoursesByUserNo(userNo: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE userNo = :userNo AND semesterId = :semesterId")
    fun getCoursesByUserNoAndSemester(userNo: String, semesterId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId")
    fun getCoursesBySemester(semesterId: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    fun getCourseById(id: Long): Flow<CourseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<CourseEntity>): List<Long>

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    @Query("UPDATE courses SET userNo = :userNo WHERE userNo = ''")
    suspend fun bindUnboundCourses(userNo: String)

    @Query("DELETE FROM courses WHERE type = 0 AND semesterId = :semesterId")
    suspend fun deleteSyncedCoursesBySemester(semesterId: String)

    @Query("DELETE FROM courses WHERE type = 0")
    suspend fun deleteSyncedCourses()

    @Query("UPDATE courses SET semesterId = :semesterId WHERE type != 0 AND semesterId = ''")
    suspend fun bindManualCoursesWithoutSemester(semesterId: String)

    @Query("SELECT * FROM courses")
    suspend fun getAllCoursesOnce(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId")
    suspend fun getCoursesBySemesterOnce(semesterId: String): List<CourseEntity>

    @Transaction
    suspend fun replaceSyncedCourses(courses: List<CourseEntity>, semesterId: String) {
        deleteSyncedCoursesBySemester(semesterId)
        if (courses.isNotEmpty()) {
            insertCourses(courses)
        }
    }
}
