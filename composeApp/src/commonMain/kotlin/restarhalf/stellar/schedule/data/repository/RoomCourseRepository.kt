package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import restarhalf.stellar.schedule.data.local.dao.CourseDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.port.JwxtAuthPort
import restarhalf.stellar.schedule.domain.port.TermSettingsPort
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class RoomCourseRepository(
    private val courseDao: CourseDao,
    private val settings: TermSettingsPort,
    private val auth: JwxtAuthPort,
) : CourseRepository {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeAllCourses(): Flow<List<Course>> =
        combine(
            settings.observeActiveScheduleTerm(),
            auth.observeProfile().map { it.userNo },
        ) { semesterId, userNo ->
            semesterId to userNo
        }.distinctUntilChanged().flatMapLatest { (semesterId, userNo) ->
            if (userNo.isBlank()) {
                flowOf(emptyList())
            } else {
                val entities = if (semesterId.isNotBlank()) {
                    courseDao.getCoursesByUserNoAndSemester(userNo, semesterId)
                } else {
                    courseDao.getCoursesByUserNo(userNo)
                }
                entities.map { list -> list.map { it.toDomain() } }
                    .onStart { emit(emptyList()) }
            }
        }

    override fun observeCourseById(id: Long): Flow<Course?> =
        combine(
            courseDao.getCourseById(id),
            settings.observeActiveScheduleTerm(),
            auth.observeProfile().map { it.userNo },
        ) { entity, semesterId, userNo ->
            entity?.toDomain()?.takeIf { course ->
                userNo.isNotBlank() &&
                    course.userNo == userNo &&
                    (semesterId.isBlank() || course.semesterId == semesterId)
            }
        }

    override suspend fun insertCourse(course: Course): Long =
        courseDao.insertCourse(course.toEntity())

    override suspend fun insertCourses(courses: List<Course>): List<Long> =
        courseDao.insertCourses(courses.map { it.toEntity() })

    override suspend fun deleteCourse(course: Course) {
        val userNo = auth.observeProfile().first().userNo
        require(userNo.isNotBlank()) { "请先登录" }
        courseDao.deleteCourseByIdAndUserNo(course.id, userNo)
    }

    override suspend fun replaceCourses(toDelete: List<Course>, toInsert: List<Course>) =
        courseDao.replaceCourses(
            toDelete = toDelete.map { it.toEntity() },
            toInsert = toInsert.map { it.toEntity() },
        )

    override suspend fun getAllCoursesOnce(): List<Course> {
        val (semesterId, userNo) = combine(
            settings.observeActiveScheduleTerm(),
            auth.observeProfile().map { it.userNo },
        ) { semesterId, userNo -> semesterId to userNo }.first()
        if (userNo.isBlank()) return emptyList()

        val entities = if (semesterId.isNotBlank()) {
            courseDao.getCoursesByUserNoAndSemesterOnce(userNo, semesterId)
        } else {
            courseDao.getCoursesByUserNoOnce(userNo)
        }
        return entities.map { it.toDomain() }
    }

    override suspend fun getAllCoursesAcrossSemesters(): List<Course> {
        val userNo = auth.observeProfile().first().userNo
        if (userNo.isBlank()) return emptyList()
        return courseDao.getCoursesByUserNoOnce(userNo).map { it.toDomain() }
    }

    override suspend fun replaceSyncedCourses(
        userNo: String,
        semesterId: String,
        courses: List<Course>,
    ) {
        require(userNo.isNotBlank()) { "用户学号不能为空" }
        val entities = courses.map {
            it.copy(userNo = userNo, semesterId = semesterId).toEntity()
        }
        courseDao.replaceSyncedCourses(userNo, semesterId, entities)

        if (semesterId.isNotBlank()) {
            courseDao.bindManualCoursesWithoutSemester(userNo, semesterId)
            settings.setActiveScheduleTerm(semesterId)
        }
    }
}
