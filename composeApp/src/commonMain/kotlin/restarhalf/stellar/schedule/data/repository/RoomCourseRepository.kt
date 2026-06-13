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

class RoomCourseRepository(
    private val courseDao: CourseDao,
    private val settings: SettingsPort,
) : CourseRepository {
    private fun filterCoursesBySemester(courses: List<Course>, semesterId: String): List<Course> {
        if (semesterId.isBlank()) return courses
        return courses.filter { it.semesterId == semesterId }
    }

    override fun observeAllCourses(): Flow<List<Course>> =
        combine(
            courseDao.getAllCourses().map { list: List<CourseEntity> -> list.map { it.toDomain() } },
            settings.observeActiveScheduleTerm()
        ) { courses, semesterId ->
            filterCoursesBySemester(courses = courses, semesterId = semesterId)
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
        return filterCoursesBySemester(
            courses = courseDao.getAllCoursesOnce().map { it.toDomain() },
            semesterId = semesterId
        )
    }

    override suspend fun replaceSyncedCourses(courses: List<Course>, semesterId: String) {
        courseDao.replaceSyncedCourses(courses.map { it.toEntity() })
        if (semesterId.isNotBlank()) {
            courseDao.bindManualCoursesWithoutSemester(semesterId)
            settings.setActiveScheduleTerm(semesterId)
        }
    }

    override suspend fun clearAllCourses() {
        courseDao.deleteAll()
        settings.setActiveScheduleTerm("")
    }
}
