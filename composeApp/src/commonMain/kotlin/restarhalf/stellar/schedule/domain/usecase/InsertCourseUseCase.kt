package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class InsertCourseUseCase(
    private val courseRepository: CourseRepository,
) {
    suspend operator fun invoke(course: Course): Long = courseRepository.insertCourse(course)
}
