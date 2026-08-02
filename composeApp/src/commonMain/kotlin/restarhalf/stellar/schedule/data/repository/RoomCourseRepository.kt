package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.dao.CourseDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.AuthPort
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
 * @param auth 认证端口，用于获取当前用户学号
 */
class RoomCourseRepository(
    private val courseDao: CourseDao,
    private val settings: SettingsPort,
    private val auth: AuthPort,
) : CourseRepository {

    /**
     * 观察所有课程
     *
     * @return 课程列表Flow，自动按当前学期和用户过滤
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeAllCourses(): Flow<List<Course>> =
        combine(
            settings.observeActiveScheduleTerm(),
            auth.observeProfile().map { it.userNo },
        ) { semesterId, userNo ->
            semesterId to userNo
        }.distinctUntilChanged().flatMapLatest { (semesterId, userNo) ->
            val hasUser = userNo.isNotBlank()
            val hasSemester = semesterId.isNotBlank()
            when {
                hasUser && hasSemester -> courseDao.getCoursesByUserNoAndSemester(
                    userNo,
                    semesterId
                )

                hasUser -> courseDao.getCoursesByUserNo(userNo)
                hasSemester -> courseDao.getCoursesBySemester(semesterId)
                else -> courseDao.getAllCourses()
            }.map { list -> list.map { it.toDomain() } }
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
        return if (semesterId.isNotBlank()) {
            courseDao.getCoursesBySemesterOnce(semesterId).map { it.toDomain() }
        } else {
            courseDao.getAllCoursesOnce().map { it.toDomain() }
        }
    }

    /**
     * 跨学期获取全部课程（忽略激活学期过滤）。
     */
    override suspend fun getAllCoursesAcrossSemesters(): List<Course> {
        return courseDao.getAllCoursesOnce().map { it.toDomain() }
    }

    /**
     * 替换同步的课程数据
     * 
     * @param courses 新课程列表
     * @param semesterId 学期ID
     */
    override suspend fun replaceSyncedCourses(courses: List<Course>, semesterId: String) {
        courseDao.replaceSyncedCourses(courses.map { it.toEntity() }, semesterId)
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

    /** 将未绑定学号的课程绑定到指定学号 */
    override suspend fun bindUnboundCourses(userNo: String) {
        courseDao.bindUnboundCourses(userNo)
    }
}
