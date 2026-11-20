package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class DeleteCourseUseCase(private val repository: CourseRepository) {

    suspend operator fun invoke(course: Course): Boolean {
        return runCatching { repository.deleteCourse(course) }
            .onFailure { e ->
                if (e is CancellationException) throw e
            }
            .isSuccess
    }

    suspend operator fun invoke(courses: List<Course>): Boolean {
        if (courses.isEmpty()) return false
        return runCatching { courses.forEach { repository.deleteCourse(it) } }
            .onFailure { e ->
                if (e is CancellationException) throw e
            }
            .isSuccess
    }
}
