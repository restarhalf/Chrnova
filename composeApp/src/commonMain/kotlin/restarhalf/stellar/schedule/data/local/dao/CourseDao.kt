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

    @Query("SELECT * FROM courses WHERE userNo = :userNo")
    fun getCoursesByUserNo(userNo: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE userNo = :userNo AND semesterId = :semesterId")
    fun getCoursesByUserNoAndSemester(userNo: String, semesterId: String): Flow<List<CourseEntity>>

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

    @Query("UPDATE courses SET userNo = :userNo WHERE userNo = ''")
    suspend fun bindUnboundCourses(userNo: String)

    @Query("DELETE FROM courses WHERE userNo = :userNo AND semesterId = :semesterId AND type = 0")
    suspend fun deleteSyncedCoursesByUserNoAndSemester(userNo: String, semesterId: String)

    @Query("UPDATE courses SET semesterId = :semesterId WHERE userNo = :userNo AND type != 0 AND semesterId = ''")
    suspend fun bindManualCoursesWithoutSemester(userNo: String, semesterId: String)

    @Query("SELECT * FROM courses WHERE userNo = :userNo")
    suspend fun getCoursesByUserNoOnce(userNo: String): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE userNo = :userNo AND semesterId = :semesterId")
    suspend fun getCoursesByUserNoAndSemesterOnce(
        userNo: String,
        semesterId: String,
    ): List<CourseEntity>

    @Transaction
    suspend fun replaceCourses(toDelete: List<CourseEntity>, toInsert: List<CourseEntity>) {
        toDelete.forEach { deleteCourse(it) }
        if (toInsert.isNotEmpty()) {
            insertCourses(toInsert)
        }
    }

    @Transaction
    suspend fun replaceSyncedCourses(
        userNo: String,
        semesterId: String,
        courses: List<CourseEntity>,
    ) {
        deleteSyncedCoursesByUserNoAndSemester(userNo, semesterId)
        if (courses.isNotEmpty()) {
            insertCourses(courses)
        }
    }
}
