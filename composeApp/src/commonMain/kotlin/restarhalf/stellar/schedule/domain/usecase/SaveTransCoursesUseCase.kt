package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.core.log.AppLogger
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class SaveTransCoursesUseCase(private val repository: CourseRepository) {

    suspend operator fun invoke(
        originCourse: Course,
        overrideCourses: List<Course>,
    ): Boolean {
        if (overrideCourses.isEmpty()) return false

        return runCatching {
            val originKey = originCourse.remoteKey
            if (originKey.isNotBlank()) {
                val replacedWeeks = overrideCourses.flatMap { it.weeks }.toSet()
                repository
                    .getAllCoursesOnce()
                    .filter { it.type == 2 && it.originRemoteKey == originKey }
                    .filter { old -> old.weeks.any { it in replacedWeeks } }
                    .forEach { repository.deleteCourse(it) }
            }
            repository.insertCourses(overrideCourses)
        }
            .onFailure { e ->
                if (e is CancellationException) throw e
                AppLogger.log("Course", "保存调课失败", e)
            }
            .isSuccess
    }
}
