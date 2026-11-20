package restarhalf.stellar.schedule.domain.usecase

import kotlinx.coroutines.CancellationException
import restarhalf.stellar.schedule.domain.model.Course
import restarhalf.stellar.schedule.domain.policy.familyKey
import restarhalf.stellar.schedule.domain.policy.isTransOverrideOf
import restarhalf.stellar.schedule.domain.repository.CourseRepository

class SaveTransCoursesUseCase(private val repository: CourseRepository) {

    suspend operator fun invoke(
        originCourse: Course,
        overrideCourses: List<Course>,
        clearedWeeks: Collection<Int> = emptyList(),
    ): Boolean {
        if (overrideCourses.isEmpty() && clearedWeeks.isEmpty()) return false

        return runCatching {
            val familyKey = originCourse.familyKey()
            val stale =
                if (familyKey.isBlank()) {
                    emptyList()
                } else {
                    val replacedOriginWeeks =
                        overrideCourses.flatMap { it.weeks }.toSet() + clearedWeeks
                    val replacedTargetWeeks = overrideCourses.map { it.targetWeek }.toSet()
                    repository
                        .getAllCoursesOnce()
                        .filter { it.isTransOverrideOf(familyKey) }
                        .filter { old ->
                            old.weeks.any { it in replacedOriginWeeks } ||
                                old.targetWeek in replacedTargetWeeks
                        }
                }
            repository.replaceCourses(toDelete = stale, toInsert = overrideCourses)
        }
            .onFailure { e ->
                if (e is CancellationException) throw e
            }
            .isSuccess
    }
}
