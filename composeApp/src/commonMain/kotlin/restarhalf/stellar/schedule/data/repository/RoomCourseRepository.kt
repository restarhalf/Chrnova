package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.Course as CourseEntity
import restarhalf.stellar.schedule.data.local.CourseDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.SettingsPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

/**
 * Room课程仓库实现类
 * 
 * 实现CourseRepository接口，负责课程数据的本地存储和查询。
 * 使用Room数据库进行数据持久化。
 * 
 * @param courseDao 课程DAO
 * @param settings 设置端口，用于获取当前学期
 */
class RoomCourseRepository(
    private val courseDao: CourseDao,
    private val settings: SettingsPort,
) : CourseRepository {

    /**
     * 按学期过滤课程
     * 
     * @param courses 课程列表
     * @param semesterId 学期ID
     * @return 过滤后的课程列表
     */
    private fun filterCoursesBySemester(courses: List<Course>, semesterId: String): List<Course> {
        if (semesterId.isBlank()) return courses
        return courses.filter { it.semesterId == semesterId }
    }

    /**
     * 观察所有课程
     * 
     * @return 课程列表Flow，自动按当前学期过滤
     */
    override fun observeAllCourses(): Flow<List<Course>> =
        combine(
            courseDao.getAllCourses().map { list: List<CourseEntity> -> list.map { it.toDomain() } },
            settings.observeActiveScheduleTerm()
        ) { courses, semesterId ->
            filterCoursesBySemester(courses = courses, semesterId = semesterId)
        }

    /**
     * 根据ID观察课程
     * 
     * @param id 课程ID
     * @return 课程Flow
     */
    override fun observeCourseById(id: Long): Flow<Course?> =
        combine(
            courseDao.getCourseById(id).map { it?.toDomain() },
            settings.observeActiveScheduleTerm()
        ) { course, semesterId ->
            course?.takeIf { semesterId.isBlank() || it.semesterId == semesterId }
        }

    /**
     * 插入课程
     * 
     * @param course 课程数据
     * @return 插入后的课程ID
     */
    override suspend fun insertCourse(course: Course): Long =
        courseDao.insertCourse(course.toEntity())

    /**
     * 删除课程
     * 
     * @param course 课程数据
     */
    override suspend fun deleteCourse(course: Course) = courseDao.deleteCourse(course.toEntity())

    /**
     * 一次性获取所有课程
     * 
     * @return 课程列表，按当前学期过滤
     */
    override suspend fun getAllCoursesOnce(): List<Course> {
        val semesterId = settings.observeActiveScheduleTerm().first()
        return filterCoursesBySemester(
            courses = courseDao.getAllCoursesOnce().map { it.toDomain() },
            semesterId = semesterId
        )
    }

    /**
     * 替换同步的课程数据
     * 
     * @param courses 新课程列表
     * @param semesterId 学期ID
     */
    override suspend fun replaceSyncedCourses(courses: List<Course>, semesterId: String) {
        courseDao.replaceSyncedCourses(courses.map { it.toEntity() })
        // 为没有学期ID的手动课程绑定学期
        if (semesterId.isNotBlank()) {
            courseDao.bindManualCoursesWithoutSemester(semesterId)
            settings.setActiveScheduleTerm(semesterId)
        }
    }

    /**
     * 清除所有课程数据
     */
    override suspend fun clearAllCourses() {
        courseDao.deleteAll()
        settings.setActiveScheduleTerm("")
    }
}
