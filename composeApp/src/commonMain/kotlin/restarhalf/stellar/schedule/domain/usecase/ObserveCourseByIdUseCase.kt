package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.flow.Flow
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class ObserveCourseByIdUseCase(private val repository: CourseRepository) {

    operator fun invoke(courseId: Long): Flow<Course?> = repository.observeCourseById(courseId)
}
