package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class DeleteCourseUseCase(private val repository: CourseRepository) {

    suspend operator fun invoke(course: Course): Boolean {
        return runCatching { repository.deleteCourse(course) }
            .onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Course", "删除课程失败", e)
            }
            .isSuccess
    }
}
