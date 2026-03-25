package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class ObserveAllCoursesUseCase(
    private val courseRepository: CourseRepository,
) {
    operator fun invoke(): Flow<List<Course>> = courseRepository.observeAllCourses()
}
