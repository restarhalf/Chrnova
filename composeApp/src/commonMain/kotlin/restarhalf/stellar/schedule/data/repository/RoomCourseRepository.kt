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

    
    override fun observeCourseById(id: Long): Flow<Course?> =
        combine(
            courseDao.getCourseById(id).map { it?.toDomain() },
            settings.observeActiveScheduleTerm()
        ) { course, semesterId ->
            course?.takeIf { semesterId.isBlank() || it.semesterId == semesterId }
        }

    
    override suspend fun insertCourse(course: Course): Long =
        courseDao.insertCourse(course.toEntity())

    
    override suspend fun deleteCourse(course: Course) = courseDao.deleteCourse(course.toEntity())

    
    override suspend fun getAllCoursesOnce(): List<Course> {
        val semesterId = settings.observeActiveScheduleTerm().first()
        return if (semesterId.isNotBlank()) {
            courseDao.getCoursesBySemesterOnce(semesterId).map { it.toDomain() }
        } else {
            courseDao.getAllCoursesOnce().map { it.toDomain() }
        }
    }

    
    override suspend fun getAllCoursesAcrossSemesters(): List<Course> {
        return courseDao.getAllCoursesOnce().map { it.toDomain() }
    }

    
    override suspend fun replaceSyncedCourses(courses: List<Course>, semesterId: String) {
        courseDao.replaceSyncedCourses(courses.map { it.toEntity() }, semesterId)

        if (semesterId.isNotBlank()) {
            courseDao.bindManualCoursesWithoutSemester(semesterId)
            settings.setActiveScheduleTerm(semesterId)
        }
    }

    
    override suspend fun clearAllCourses() {
        courseDao.deleteAll()
        settings.setActiveScheduleTerm("")
    }

    
    override suspend fun bindUnboundCourses(userNo: String) {
        courseDao.bindUnboundCourses(userNo)
    }
}
