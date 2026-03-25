package restarhalf.stellar.schedule.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import restarhalf.stellar.schedule.data.local.CourseDao
import restarhalf.stellar.schedule.data.mapper.toDomain
import restarhalf.stellar.schedule.data.mapper.toEntity
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository
import restarhalf.stellar.schedule.data.local.Course as CourseEntity

class RoomCourseRepository(private val courseDao: CourseDao) : CourseRepository {
    override fun observeAllCourses(): Flow<List<Course>> =
        courseDao.getAllCourses().map { list: List<CourseEntity> -> list.map { it.toDomain() } }

    override fun observeCourseById(id: Long): Flow<Course?> =
        courseDao.getCourseById(id).map { it?.toDomain() }

    override suspend fun insertCourse(course: Course): Long =
        courseDao.insertCourse(course.toEntity())

    override suspend fun deleteCourse(course: Course) = courseDao.deleteCourse(course.toEntity())

    override suspend fun getAllCoursesOnce(): List<Course> =
        courseDao.getAllCoursesOnce().map { it.toDomain() }
}
