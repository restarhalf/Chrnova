package restarhalf.stellar.schedule.domain.usecase

import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class DeleteCourseUseCase(private val courseRepository: CourseRepository) {
    suspend operator fun invoke(course: Course) {
        courseRepository.deleteCourse(course)
    }
}
