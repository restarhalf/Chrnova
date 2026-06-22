package restarhalf.stellar.schedule.domain.repository

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Course

/**
 * 课程仓库接口
 * 
 * 定义课程数据的访问抽象接口。
 */
interface CourseRepository {
    /**
     * 观察所有课程
     * 
     * @return 课程列表Flow
     */
    fun observeAllCourses(): Flow<List<Course>>

    /**
     * 根据ID观察课程
     * 
     * @param id 课程ID
     * @return 课程Flow
     */
    fun observeCourseById(id: Long): Flow<Course?>

    /**
     * 插入课程
     * 
     * @param course 课程数据
     * @return 插入后的课程ID
     */
    suspend fun insertCourse(course: Course): Long

    /**
     * 删除课程
     * 
     * @param course 课程数据
     */
    suspend fun deleteCourse(course: Course)

    /**
     * 一次性获取所有课程
     * 
     * @return 课程列表
     */
    suspend fun getAllCoursesOnce(): List<Course>

    /**
     * 替换同步的课程数据
     * 
     * @param courses 新课程列表
     * @param semesterId 学期ID
     */
    suspend fun replaceSyncedCourses(courses: List<Course>, semesterId: String)

    /**
     * 清除所有课程
     */
    suspend fun clearAllCourses()

    /**
     * 将未绑定学号的课程绑定到指定学号
     *
     * @param userNo 学号
     */
    suspend fun bindUnboundCourses(userNo: String)
}
