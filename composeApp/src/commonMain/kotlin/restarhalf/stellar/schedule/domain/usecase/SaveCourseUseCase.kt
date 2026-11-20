package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class SaveCourseUseCase(private val repository: CourseRepository) {

    suspend operator fun invoke(course: Course): Boolean {
        return runCatching { repository.insertCourse(course) }
            .onFailure { e ->
                if (e is CancellationException) throw e
            }
            .isSuccess
    }
}
