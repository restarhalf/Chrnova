package restarhalf.stellar.schedule.data.local

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/**
 * 课程数据访问对象（DAO）
 * 
 * 定义课程表的所有数据库操作。
 */
@Dao
interface CourseDao {

    /**
     * 观察所有课程
     * 
     * @return 课程列表Flow
     */
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    /**
     * 根据ID观察课程
     * 
     * @param id 课程ID
     * @return 课程Flow
     */
    @Query("SELECT * FROM courses WHERE id = :id LIMIT 1")
    fun getCourseById(id: Long): Flow<Course?>

    /**
     * 插入课程
     * 
     * @param course 课程数据
     * @return 插入后的课程ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    /**
     * 批量插入课程
     * 
     * @param courses 课程列表
     * @return 插入后的课程ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>): List<Long>

    /**
     * 删除课程
     * 
     * @param course 课程数据
     */
    @Delete
    suspend fun deleteCourse(course: Course)

    /**
     * 更新课程
     * 
     * @param course 课程数据
     */
    @Update
    suspend fun updateCourse(course: Course)

    /** 删除所有课程 */
    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    /** 删除所有同步的课程（type=0） */
    @Query("DELETE FROM courses WHERE type = 0")
    suspend fun deleteSyncedCourses()

    /**
     * 为没有学期ID的手动课程绑定学期
     * 
     * @param semesterId 学期ID
     */
    @Query("UPDATE courses SET semesterId = :semesterId WHERE type != 0 AND semesterId = ''")
    suspend fun bindManualCoursesWithoutSemester(semesterId: String)

    /**
     * 一次性获取所有课程
     * 
     * @return 课程列表
     */
    @Query("SELECT * FROM courses")
    suspend fun getAllCoursesOnce(): List<Course>

    /**
     * 替换同步的课程数据
     * 
     * 先删除所有同步课程，再插入新课程。
     * 
     * @param courses 新课程列表
     */
    @Transaction
    suspend fun replaceSyncedCourses(courses: List<Course>) {
        deleteSyncedCourses()
        if (courses.isNotEmpty()) {
            insertCourses(courses)
        }
    }
}
